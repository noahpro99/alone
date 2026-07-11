# Alone Wiki

The documentation site for **Alone**, built with [Docusaurus](https://docusaurus.io/).
All project docs — the design proposal, engineering plan, and progress backlog — live in
`docs/` and are published to GitHub Pages at **https://noahpro99.github.io/alone/**.

## Local development

Node 18+ is required. Inside the repo's Nix dev shell (`nix develop`) Node is already on the
`PATH`; otherwise install Node yourself.

```bash
cd wiki
npm install        # first time only
npm start          # live-reloading dev server at http://localhost:3000/alone/
npm run build      # production build into ./build (what CI deploys)
npm run serve      # serve the production build locally
```

## Deployment

Pushing to `main` with changes under `wiki/` triggers `.github/workflows/deploy.yml`, which
builds the site and publishes it to GitHub Pages. No manual deploy step is needed.

> **One-time repo setup:** in the GitHub repo, go to **Settings → Pages → Build and deployment**
> and set the source to **GitHub Actions**.

## Structure

```
docs/
  intro.md              Overview / landing page (served at /)
  getting-started/      Dev quickstart, building the mrpack, project layout
  proposal/             The full design proposal, one page per section (§0–§14 + appendices)
  engineering/          Toolchain, architecture, phased rollout, dev workflow
  progress/             Feature backlog and remaining-content plan
```

Ordering is controlled by `sidebar_position` frontmatter and each folder's `_category_.json`.
