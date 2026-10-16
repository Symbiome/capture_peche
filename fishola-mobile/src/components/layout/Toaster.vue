<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
  <div class="toaster" v-bind:class="visibility">
    <div class="toaster-box" v-bind:class="level">
      <div>
        <i
          :class="{
            'icon-success': level == 'success',
            'icon-warning': level == 'warning',
            'icon-error': level == 'error',
          }"
        />
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Helpers from "@/services/Helpers";
import { CARNET_COLORS } from "@/services/CarnetPalette";

import { Component, Vue } from "vue-facing-decorator";
import EventBus from "@/services/EventBus";
import { StatusBar } from "@capacitor/status-bar";

@Component
export default class Toaster extends Vue {
  visibility: string = "toaster-hidden";
  level: string = "";
  message: string = "";

  mounted() {
    EventBus.on("toaster-error", (text: string, durationArg?: number) => {
      const duration = durationArg || 2000;
      this.newError(text, duration);
    });
    EventBus.on("toaster-warning", (text: string, durationArg?: number) => {
      const duration = durationArg || 2000;
      this.newWarning(text, duration);
    });
    EventBus.on("toaster-success", (text: string, durationArg?: number) => {
      const duration = durationArg || 2000;
      this.newSuccess(text, duration);
    });
  }

  beforeUnmount() {
    console.debug("Destroy toaster");
    EventBus.off("toaster-error");
    EventBus.off("toaster-warning");
    EventBus.off("toaster-success");
  }

  newError(text: string, duration: number) {
    this.newMessage(text, "error", duration);
  }

  newWarning(text: string, duration: number) {
    this.newMessage(text, "warning", duration);
  }

  newSuccess(text: string, duration: number) {
    this.newMessage(text, "success", duration);
  }

  newMessage(text: string, level: string, duration: number) {
    this.message = text;
    this.level = level;
    this.visibility = "toaster-visible";
    let statusBarColor = CARNET_COLORS.darkGreen;
    if (level === "warning") {
      statusBarColor = CARNET_COLORS.orange;
    } else if (level === "error") {
      statusBarColor = CARNET_COLORS.red;
    }
    Helpers.ifApplication(() => {
      StatusBar.setBackgroundColor({ color: statusBarColor });
    });
    setTimeout(this.resetToaster, 2 * 500 + duration);
  }

  resetToaster() {
    this.visibility = "toaster-disappears";
    Helpers.ifApplication(() => {
      StatusBar.setBackgroundColor({ color: CARNET_COLORS.blue });
    });
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="less">
.toaster-hidden {
  top: calc(-1 * @toaster-height);
}

.toaster-disappears {
  animation-duration: 0.5s;
  animation-name: disappear;

  top: calc(-1 * @toaster-height);

  @keyframes disappear {
    from {
      top: 0px;
    }
    to {
      top: calc(-1 * @toaster-height);
    }
  }
}

.toaster-visible {
  animation-duration: 0.5s;
  animation-name: appear;

  top: 0px;

  @keyframes appear {
    from {
      top: calc(-1 * @toaster-height);
    }
    to {
      top: 0px;
    }
  }
}

.toaster {
  position: absolute;
  left: 0px;
  width: 100%;
  height: @toaster-height;
  z-index: 999;

  display: flex;
  justify-content: center;
  align-items: center;

  .toaster-box {
    width: 100%;
    font-weight: 600;
    height: 100%;

    display: flex;
    justify-content: center;
    align-items: center;

    div {
      font-size: @fontsize-toaster;
      line-height: calc(@fontsize-toaster + @line-height-padding-medium);

      i {
        margin-right: @vertical-margin-x-small;
        font-size: @fontsize-toaster;
      }
    }
  }

  .toaster-box.error {
    color: @white;
    background: @error;
  }

  .toaster-box.warning {
    color: @carnet-navy;
    background: @warning;
  }

  .toaster-box.success {
    color: @white;
    background: @success;
  }
}

@media screen and (min-width: @desktop-min-width) {
  .toaster-hidden {
    top: calc(-1 * @toaster-height-desktop);
  }

  .toaster-disappears {
    top: calc(-1 * @toaster-height-desktop);

    @keyframes disappear {
      from {
        top: 0px;
      }
      to {
        top: calc(-1 * @toaster-height-desktop);
      }
    }
  }

  .toaster-visible {
    @keyframes appear {
      from {
        top: calc(-1 * @toaster-height-desktop);
      }
      to {
        top: 0px;
      }
    }
  }

  .toaster {
    height: @toaster-height-desktop;

    .toaster-box {
      div {
        font-size: @fontsize-toaster-desktop;
        line-height: calc(
          @fontsize-toaster-desktop + @line-height-padding-medium
        );

        i {
          font-size: @fontsize-toaster-desktop;
        }
      }
    }
  }
}
</style>
