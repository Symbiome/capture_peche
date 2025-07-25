<!--
  #%L
  Fishola :: Admin
  %%
  Copyright (C) 2019 - 2022 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<template>
  <div>
    <input
      ref="upload"
      type="file"
      id="avatar"
      name="avatar"
      accept="image/png, image/jpeg"
      @change="uploadImageFile"
    />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import BackendService from "@/services/BackendService";
import Constants from "@/services/Constants";

@Component
export default class ImageUploader extends Vue {
  @Prop() itemId: string;
  @Prop() isMiniature: boolean;

  async uploadImageFile(e: InputEvent): Promise<void> {
    const uploadedFile = this.$refs.upload as HTMLInputElement;
    if (uploadedFile.files) {
      const file = uploadedFile.files[0];
      const base64 = await this.getBase64(file);
      const id = this.itemId ? this.itemId : "temp-id";
      let url = "/v1/news-picture/";
      if (this.isMiniature) {
        url = "/v1/news-miniature/";
      }
      const newsPicture = await BackendService.backendPost(url + id, base64);
      const newsPicturesURL = Constants.apiUrl(
        "/v1/news-picture/" + newsPicture["id"]
      );
      this.$emit("uploaded-pic", newsPicturesURL);
    }
  }

  getBase64(file: any): Promise<string> {
    return new Promise<any>((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = function() {
        resolve(reader.result);
      };
      reader.onerror = function(error) {
        reject(error);
      };
    });
  }
}
</script>
