import babel from "@rollup/plugin-babel";
import sizes from "rollup-plugin-sizes";
import pkg from "./package.json" assert { type: "json" };

const config = [
  // commonjs build exposed in "main" entry of package.json
  {
    input: "dist/index.mjs",
    output: {
      file: pkg.main,
      format: "cjs",
      name: "@airbus/icons",
    },
  },
  // react esm entry point
  {
    input: "react/index.js",
    output: {
      file: "react/index.esm.js",
      format: "esm",
      exports: "named",
      name: "@airbus/icons/react",
      banner: "import React from 'react';",
    },
    external: [/@babel\/runtime/, "react"],
    plugins: [
      sizes(),
      babel({
        babelHelpers: "runtime",
        plugins: ["@babel/plugin-transform-runtime"],
        presets: [
          ["@babel/preset-env", { targets: "defaults" }],
          "@babel/preset-react",
        ],
      }),
    ],
  },
  // react cjs entry point
  {
    input: "react/index.js",
    output: {
      file: "react/index.cjs.js",
      format: "cjs",
      name: "@airbus/icons/react",
    },
    external: [/@babel\/runtime/, "react"],
    plugins: [
      sizes(),
      babel({
        babelHelpers: "runtime",
        plugins: ["@babel/plugin-transform-runtime"],
        presets: [
          ["@babel/preset-env", { targets: "defaults" }],
          "@babel/preset-react",
        ],
      }),
    ],
  },
];

export default config;
