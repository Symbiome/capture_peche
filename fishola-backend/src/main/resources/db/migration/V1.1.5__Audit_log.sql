-- Journal global des actions (CdC 3.1.5.1 ; archivage RGPD note Q8).
-- Écriture applicative via intercepteur JAX-RS (@Audited), append-only :
-- aucune UPDATE/DELETE fonctionnelle (la purge de rétention se fait par tâche
-- dédiée, cf. #11 §4.3). Pas de FK dure vers fishola_admin/fishola_user pour
-- que la trace survive à l'anonymisation/suppression de l'acteur.
CREATE TABLE public.audit_log (
    id           uuid          NOT NULL DEFAULT gen_random_uuid(),
    actor_type   varchar(16)   NOT NULL,   -- 'admin' | 'user' | 'system'
    actor_id     uuid          NULL,        -- fishola_admin.id / fishola_user.id ; NULL si 'system'
    action       varchar(64)   NOT NULL,    -- ex: 'trip.export', 'admin.create', 'catch.update'
    entity_type  varchar(64)   NULL,        -- ex: 'trip', 'catch', 'fishola_admin', 'species'
    entity_id    uuid          NULL,        -- id de la ressource visée (si connue)
    at           timestamptz   NOT NULL DEFAULT now(),
    details      jsonb         NOT NULL DEFAULT '{}'::jsonb,  -- method, path, httpStatus, scope…
    CONSTRAINT audit_log_pkey PRIMARY KEY (id),
    CONSTRAINT audit_log_actor_type_chk CHECK (actor_type IN ('admin','user','system'))
);

COMMENT ON TABLE public.audit_log IS
    'Journal append-only des actions (CdC 3.1.5.1 / RGPD note Q8). Écrit par l''intercepteur @Audited sur les mutations et exports réussis.';

-- Consultation depuis le module admin : filtres temporels / par acteur / par action.
CREATE INDEX audit_log_at_idx     ON public.audit_log (at DESC);
CREATE INDEX audit_log_actor_idx  ON public.audit_log (actor_type, actor_id, at DESC);
CREATE INDEX audit_log_action_idx ON public.audit_log (action, at DESC);
CREATE INDEX audit_log_entity_idx ON public.audit_log (entity_type, entity_id);
CREATE INDEX audit_log_details_gin ON public.audit_log USING gin (details);
