package fr.inrae.fishola.rest.editorial;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.UUID;

@JsonSerialize(as = DocumentationWithBase64ContentBean.class)
public class DocumentationWithBase64ContentBean implements DocumentationLight {
    private UUID id;
    private String naturalId;
    private String name;
    private String url;
    private String base64Content;

    public UUID id() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String naturalId() {
        return naturalId;
    }

    public void setNaturalId(String naturalId) {
        this.naturalId = naturalId;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String url() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String base64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }
}
