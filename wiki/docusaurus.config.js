// @ts-check
// Docusaurus config for the Alone wiki. See https://docusaurus.io/docs/api/docusaurus-config
import {themes as prismThemes} from 'prism-react-renderer';
import {createRequire} from 'node:module';

// This config is an ES module, so `require` isn't defined by default. The local
// search theme wants an absolute path via require.resolve — shim one in.
const require = createRequire(import.meta.url);

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Alone',
  tagline: 'A survival-realism overhaul for Minecraft — a game about maintenance, not accumulation.',
  favicon: 'img/logo.svg',

  // Deployed to GitHub Pages at https://noahpro99.github.io/alone/
  url: 'https://noahpro99.github.io',
  baseUrl: '/alone/',

  organizationName: 'noahpro99', // GitHub org/user
  projectName: 'alone', // repo name
  trailingSlash: false,

  // Serve the mod's own item/block textures straight from the source tree at build time, so the
  // wiki can show real in-game art WITHOUT copying any PNGs into wiki/ (they'd bloat the repo).
  // Files land at the site root: e.g. modules/.../textures/item/waterskin.png -> /alone/item/waterskin.png
  staticDirectories: ['static', '../modules/core/src/main/resources/assets/alone/textures'],

  onBrokenLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    // Parse .md files as CommonMark (not MDX) so imported prose with `<name>`-style
    // tokens and other JSX-hostile characters renders without breaking the build.
    format: 'detect',
  },

  themes: [
    [
      // Offline/local search — builds the index at compile time, runs entirely
      // client-side. No external service, API keys, or crawler required.
      require.resolve('@easyops-cn/docusaurus-search-local'),
      /** @type {import('@easyops-cn/docusaurus-search-local').PluginOptions} */
      ({
        hashed: true, // cache-bust the index across builds
        indexBlog: false, // no blog on this site
        docsRouteBasePath: '/', // docs are served at the site root
        highlightSearchTermsOnTargetPage: true,
        searchResultLimits: 8,
        searchBarShortcutHint: true,
      }),
    ],
  ],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          // Docs-only mode: the wiki lives at the site root, no separate landing page.
          routeBasePath: '/',
          sidebarPath: './sidebars.js',
          editUrl: 'https://github.com/noahpro99/alone/tree/main/wiki/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Alone',
        logo: {
          alt: 'Alone modpack logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'wikiSidebar',
            position: 'left',
            label: 'Wiki',
          },
          {
            href: 'https://github.com/noahpro99/alone',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Wiki',
            items: [
              {label: 'Overview', to: '/'},
              {label: 'Download', to: '/download'},
              {label: 'Features', to: '/features/survival-meters'},
              {label: 'Roadmap', to: '/roadmap'},
              {label: 'Engineering', to: '/engineering/toolchain'},
            ],
          },
          {
            title: 'Project',
            items: [
              {label: 'GitHub', href: 'https://github.com/noahpro99/alone'},
              {label: 'Issues', href: 'https://github.com/noahpro99/alone/issues'},
            ],
          },
        ],
        copyright: `Alone — a survival-realism overhaul for Minecraft. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['java', 'groovy', 'json', 'bash', 'nix'],
      },
    }),
};

export default config;
