module.exports = function(on, config) {
  on("task", {
    log(message) {
      console.log(message);
      return null;
    },
  });

  // Le reporter mochawesome (et son merge after:run) ne s'active QUE si le run
  // l'utilise réellement — sinon la fusion plante faute de JSON par spec
  // (cas de `cypress:run` / `cypress:bench`, qui tournent en reporter `spec`).
  if (typeof config.reporter === "string" && config.reporter.includes("mochawesome")) {
    require("cypress-mochawesome-reporter/plugin")(on);
  }

  return config;
};
