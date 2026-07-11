// @ts-check
// Docusaurus config for the Alone wiki. See https://docusaurus.io/docs/api/docusaurus-config
import {themes as prismThemes} from 'prism-react-renderer';

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
              {label: 'Design Proposal', to: '/proposal/philosophy'},
              {label: 'Engineering', to: '/engineering/toolchain'},
              {label: 'Progress', to: '/progress/backlog'},
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
