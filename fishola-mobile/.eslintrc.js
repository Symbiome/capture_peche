module.exports = {
  root: true,
  env: {
    node: true,
    es2022: true
  },
  extends: ["plugin:vue/essential", "eslint:recommended", "@vue/typescript"],
  rules: {
    "no-console": "off",
    "no-unused-vars": "off",
    "@typescript-eslint/no-unused-vars": [
      "warn",
      {
        argsIgnorePattern: "^_",
        varsIgnorePattern: "^_",
        caughtErrorsIgnorePattern: "^_",
      },
    ],
  },
  parserOptions: {
    parser: "@typescript-eslint/parser",
  },
  overrides: [
    {
      files: ["**/__tests__/*.{j,t}s?(x)", "**/tests/**/*.test.{j,t}s?(x)"],
      env: {
        jest: true,
      },
    },
  ],
};
