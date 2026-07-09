package fr.inrae.fishola.rest.editorial;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
import java.util.Set;
import java.util.UUID;

public class NewsBean {

    public UUID id;
    public String name;
    public String content;
    public LocalDateTime datePublicationDebut;
    public LocalDateTime datePublicationFin;
    public LocalDateTime dateNotificationSent;
    public UUID miniatureId;
    public Boolean isNational;
    public Set<UUID> waterEntityIds;

    public NewsBean(UUID id, String name, String content, LocalDateTime datePublicationDebut, LocalDateTime datePublicationFin, LocalDateTime dateNotificationSent, UUID miniatureId, Boolean isNational, Set<UUID> waterEntityIds) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.datePublicationDebut = datePublicationDebut;
        this.datePublicationFin = datePublicationFin;
        this.dateNotificationSent = dateNotificationSent;
        this.miniatureId = miniatureId;
        this.isNational = isNational;
        this.waterEntityIds = waterEntityIds;
    }
}
