# Airbus Design System Icons

## Introduction

This package provides all icons from the Airbus Design System, as available to designers in Figma.

You can browse the available icons here: 
- **CSS**: https://css.design-system.airbus.corp/?path=/docs/general-visual-styles-icons--airbus
- **React**: "https://react.design-system.airbus.corp/?path=/docs/general-visual-styles-icons--docs
- **Angular**: https://angular.design-system.airbus.corp/?path=/docs/general-visual-styles-icons--docs

Starting with v3, and as described in this [RFC](https://github.airbus.corp/Airbus/design-system/discussions/2472), the included [Google Material Icons](https://fonts.google.com/icons) are now limited to those used internally by Design System components.

For other Material Icons or Symbols, refer to the documentation: https://css.design-system.airbus.corp/?path=/docs/general-visual-styles-icons--airbus#material-icons

## 3rd party licenses

Dev dependencies of this package are used only for development and build purposes, and are not included in the final output.

Projects consuming this package do not have to include them, and are not subject to their licenses.

For projects importing this package, the following 3rd party licenses apply:

- [Material Icons (apache-2.0)](https://developers.google.com/fonts/docs/material_icons#licensing)

## Getting the library

### NPM

`npm install @airbus/icons`

For the setup of your development environment, see https://github.airbus.corp/uxid/airbus-design-system#environment-setup

### Download

The npm package is available as a zip file from the latest release: https://github.airbus.corp/uxid/ds-icons/releases

## Usage

By order of priority, we recommend these approaches:

- **React**: if you are using React 😉
- **SVG Sprite**: better than a font for scaling and accessibility, but requires a polyfill for some browsers
- **Webfont**: easy to set up, and works everywhere
- **SVGs**: you can include individual SVGs in your project, by building your own font or SVG Sprite, or using SVG loaders
- **JS**: if your build system supports importing JavaScript modules with tree shaking, and you want to manipulate SVG data

### React

React components are available for each icon.

```js
import { Search } from "@airbus/icons/react";

function MyComponent(props) {
  return (
    <div>
      Search icon: <Search />
    </div>
  );
}
```

### SVG SPRITE

The SVG Sprite file can be used like this:

`<svg role="img"><use xlink:href="./airbus-icons.svg#bookmark" /></svg>`

For IE and other incompatible browsers you should use this polyfill: https://github.com/jonathantneal/svg4everybody

### Webfont

Include the stylesheet and the fonts in your static assets.
Then you can use the icons like this:

`<i class="ds-icon-3d_rotation"></i>`

### SVGs

Individual SVGs are available for you to include in your project in any possible way.

Here are some approaches:

- Webpack SVG url loader: https://github.com/bhovhannes/svg-url-loader
- Webpack SVG Sprite loader: https://github.com/JetBrains/svg-sprite-loader
- SVG Sprite generator: https://github.com/svgstore/svgstore
- SVG to React component: https://react-svgr.com/
- Create-react-app: https://create-react-app.dev/docs/adding-images-fonts-and-files/#adding-svgs
- Angular SVG as templates: https://angular.io/guide/svg-in-templates

### JS

⚠ Your build system has to support tree shaking. Otherwise, you will end up with all icons bundled in your final application.
Tree shaking is supported by JavaScript bundlers like rollup (https://rollupjs.org/guide/en/#tree-shaking) or webpack (https://webpack.js.org/guides/tree-shaking/).

The JavaScript modules provide SVG Icons as JavaScript objects with two properties:

- name: svg name
- data: svg data as a string

```js
import { IconSearch } from "@airbus/icons";

// convert svg string to base64 dataUri
const btoa =
  typeof window !== "undefined"
    ? window.btoa
    : (str: string) => Buffer.from(str).toString("base64");

export const svgUri = (svgString: string) =>
  `url('data:image/svg+xml;base64, ${btoa(svgString)}')`;

// CSSinJS styles
const styles = {
  backgroundImage: svgUri(IconSearch.data),
};
```

## Contributing

Read the [Contribution guide](CONTRIBUTING.md) to learn about our development process, how to propose bugfixes and improvements, and how to build and test your changes.

## Changelog

Read the [changelog](https://github.airbus.corp/uxid/ds-icons/releases) for details on what has changed recently.
