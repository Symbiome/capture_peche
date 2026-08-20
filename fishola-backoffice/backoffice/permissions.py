"""
Widget « matrice de droits » pour l'édition des permissions d'un groupe.

Remplace la double-liste par défaut (illisible, libellés « Can add … » en anglais)
par une matrice : une ligne par modèle, une colonne par action
(Ajout / Modif. / Suppr. / Visu.), en français. Les permissions non standard
(hors add/change/delete/view) sont listées à part.
"""
from django import forms
from django.apps import apps
from django.contrib.auth.models import Group, Permission
from django.utils.html import conditional_escape
from django.utils.safestring import mark_safe

_ACTIONS = [("add", "Ajout"), ("change", "Modif."), ("delete", "Suppr."), ("view", "Visu.")]
_ACTION_KEYS = {a for a, _ in _ACTIONS}

_CSS = (
    "<style>"
    ".perm-matrix{border-collapse:collapse;width:100%;font-size:.85rem}"
    ".perm-matrix th,.perm-matrix td{border:1px solid rgba(120,120,120,.25);padding:.35rem .6rem}"
    ".perm-matrix th{font-weight:600;text-align:center}"
    ".perm-matrix th.model,.perm-matrix td.model{text-align:left}"
    ".perm-matrix td.chk{text-align:center;width:5rem}"
    ".perm-matrix tr.app td{background:rgba(120,120,120,.12);font-weight:700}"
    ".perm-matrix input{width:1.05rem;height:1.05rem;cursor:pointer}"
    ".perm-others label{display:block;margin:.15rem 0}"
    "</style>"
)


class PermissionMatrixWidget(forms.SelectMultiple):
    """N'override que le rendu ; la lecture des cases cochées (value_from_datadict)
    reste celle de SelectMultiple (getlist)."""

    def render(self, name, value, attrs=None, renderer=None):
        selected = {str(v) for v in (value or [])}
        perms = list(
            Permission.objects.select_related("content_type").order_by(
                "content_type__app_label", "content_type__model", "codename"
            )
        )

        matrix, others, cts = {}, [], {}
        for p in perms:
            ct = p.content_type
            cts[ct.pk] = ct
            action = p.codename.split("_", 1)[0]
            if action in _ACTION_KEYS and p.codename == f"{action}_{ct.model}":
                matrix.setdefault(ct.pk, {})[action] = p
            else:
                others.append(p)

        def checkbox(perm):
            checked = " checked" if str(perm.pk) in selected else ""
            return f'<input type="checkbox" name="{name}" value="{perm.pk}"{checked}>'

        head = "<tr><th class='model'>Modèle</th>" + "".join(
            f"<th>{lbl}</th>" for _, lbl in _ACTIONS
        ) + "</tr>"

        body, current_app = [], None
        for ct in sorted(cts.values(), key=lambda c: (str(c.app_label), str(c.model))):
            if ct.app_label != current_app:
                current_app = ct.app_label
                try:
                    app_name = apps.get_app_config(ct.app_label).verbose_name
                except LookupError:
                    app_name = ct.app_label
                body.append(f"<tr class='app'><td colspan='5'>{conditional_escape(app_name)}</td></tr>")
            cells = "".join(
                f"<td class='chk'>{checkbox(matrix[ct.pk][a]) if matrix.get(ct.pk, {}).get(a) else ''}</td>"
                for a, _ in _ACTIONS
            )
            body.append(f"<tr><td class='model'>{conditional_escape(ct.name)}</td>{cells}</tr>")

        others_html = ""
        if others:
            items = "".join(
                f"<label>{checkbox(p)} {conditional_escape(p.name)}</label>" for p in others
            )
            others_html = f"<div class='perm-others'><p style='font-weight:600;margin:.6rem 0 .2rem'>Autres permissions</p>{items}</div>"

        table = f"<table class='perm-matrix'>{head}{''.join(body)}</table>"
        return mark_safe(_CSS + table + others_html)


class GroupAdminForm(forms.ModelForm):
    class Meta:
        model = Group
        fields = "__all__"
        widgets = {"permissions": PermissionMatrixWidget}
