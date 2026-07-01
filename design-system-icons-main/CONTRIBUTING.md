# Contributing to Airbus Icons

## Install

`npm install`

To setup your environment for git/npm see

* Git: https://github.airbus.corp/uxid/airbus-design-system/blob/master/CONTRIBUTING.md#git-setup
* NPM: https://github.airbus.corp/uxid/airbus-design-system#environment-setup

## Build

`npm run build`

Will output build artefacts in `dist` and `react` folder

* `icons`: individual icons as svg and png files grouped by categories
* `webfont`: icon font in different formats
* `svgsprite`: all icons bundled as a unique svg file
* `js`: individual icons as javascript and typescript modules
* `react`: individual icons as React components

## Serve

The webfont html template works locally without being served by a web server, but not the svgsprite one as most browsers will throw security errors.

Use this command to launch a local webserver : `npm run serve`

## Release

This repository uses semantic-release, with enforced commit conventions

* https://semantic-release.gitbook.io/semantic-release/#commit-message-format
* https://github.com/angular/angular.js/blob/master/DEVELOPERS.md#-git-commit-guidelines

Once a new version is ready in `master` branch, this command will bump the version and publish NPM/Git releases automatically.

`CI=true npm run release`

You can run a dry release with this command

`npm run release:dry`

### Major versions and prereleases

Semantic release enforce the following rules and workflow

* prereleases can only be published from "next" branch (see release.config.js)
* to bump to a new major version a commit with a breaking change needs to exist in git history of the branch
* semantic release fetch git tags to know the latest version number. So to control the future major version number, it is possible to push a new tag for semantic release to start from there.

see https://github.com/semantic-release/semantic-release/blob/master/docs/recipes/release-workflow/pre-releases.md

## Deployment

Html templates can be deployed in our github pages staging environment : https://github.airbus.corp/pages/uxid/ds-staging/

## Figma svg issues

Some svgs exported from figma do not play well when converted into a webfont.
It is a know issue. See https://github.com/tancredi/fantasticon/issues/135

Workaround: `npx svg-orient ./path/to/figma/svg/file.svg`
