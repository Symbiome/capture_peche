import VueRouter from "vue-router";

export class RouterUtils {
  static async pushRouteNoDuplicate(router: VueRouter, route: any) {
    try {
      await router.push(route);
    } catch (err) {
      if (!VueRouter.isNavigationFailure(err)) {
        console.error(err);
      }
      return err;
    }
  }
}
