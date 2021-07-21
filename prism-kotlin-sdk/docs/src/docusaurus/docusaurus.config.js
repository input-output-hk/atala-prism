const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');
const { version, branch } = require('./version');

/** @type {import('@docusaurus/types').DocusaurusConfig} */
module.exports = {
  title: 'Atala PRISM SDK',
  tagline: 'The official Software Development Kit (SDK) for the Atala PRISM project',
  url: `https://docs-${branch}.atalaprism.io`,
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'input-output-hk',
  projectName: 'atala-prism-sdk',
  customFields: {
    version: version,
  },
  themeConfig: {
    navbar: {
      logo: {
        alt: 'Atala PRISM logo',
        src: 'img/atala-prism-logo-suite-light.svg',
        srcDark: "img/atala-prism-logo-suite-dark.svg",
      },
      items: [
        {
          type: 'doc',
          docId: 'tutorials',
          position: 'left',
          label: 'Tutorials',
        },
        {
          type: 'doc',
          docId: 'modules',
          position: 'left',
          label: 'Modules',
        },
        {
          to: 'protodocs/common_models.proto',
          activeBasePath: 'protodocs',
          position: 'left',
          label: 'gRPC',
        },
        {
          href: 'https://github.com/input-output-hk/atala-prism-sdk',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Help',
          items: [
            {
              html: `
                <strong>Any problem?</strong> Contact us using <a href="mailto:atala-prism-support@iohk.io">this service email</a>.
              `,
            },
          ],
        },
        {
          title: 'Version',
          items: [
            {
              html: `Currently <a href="https://github.com/input-output-hk/atala-tobearchived/packages/864280?version=${version}">${version}</a>.`,
            },
          ]
        },
        {
          title: 'Legal',
          items: [
            {
              label: 'Data protection policy',
              href: 'https://static.iohk.io/gdpr/IOHK-Data-Protection-GDPR-Policy.pdf',
            },
          ]
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Input Output HK. All rights reserved. Built with Docusaurus.`,
    },
    prism: {
      theme: lightCodeTheme,
      darkTheme: darkCodeTheme,
      additionalLanguages: ['kotlin'],
    },
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
    [
      'docusaurus-protobuffet',
      {
        protobuffet: {
          fileDescriptorsPath: './docs/grpc/grpc-api.json',
          protoDocsPath: './protodocs',
          sidebarPath: './generatedSidebarsProtodocs.js',
        },
        docs: {
          sidebarPath: './sidebarsProtodocs.js',
        },
      }
    ],
  ],
  plugins: [
    require.resolve('docusaurus-lunr-search'),
  ]
};
