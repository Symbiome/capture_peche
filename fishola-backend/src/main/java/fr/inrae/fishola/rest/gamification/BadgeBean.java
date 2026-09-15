package fr.inrae.fishola.rest.gamification;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un badge du catalogue (#146), avec l'état de déblocage du pêcheur courant lorsque
 * renvoyé par {@link GamificationResource} ({@code unlocked = false} et les champs de
 * déblocage nuls sinon). {@link GamificationAdminResource} réutilise ce même bean pour le
 * sélecteur d'attribution manuelle (unlocked toujours faux dans ce contexte).
 */
public class BadgeBean {

    public UUID id;
    public String code;
    public String category;
    public String name;
    public String description;
    public String icon;
    public String ruleType;
    public Integer tier;
    public boolean annualReset;

    public boolean unlocked;
    public LocalDateTime unlockedAt;
    public Object context;
}
