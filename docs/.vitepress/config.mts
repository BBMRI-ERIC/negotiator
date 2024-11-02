import {defineConfig} from "vitepress"
import {execSync} from 'child_process';


function getLatestGitTag(): string {
    try {
        return execSync('git describe --tags --abbrev=0').toString().trim();
    } catch (error) {
        console.error("Error fetching Git tag:", error);
        return '0.0.0'; // Fallback version if no tags are found
    }
}

const latestVersion = getLatestGitTag();
export default defineConfig({
    title: "BBMRI-ERIC Negotiator",
    description: "Documentation for the BBMRI-ERIC Negotiator",
    head: [['link', {rel: 'icon', href: '/handshake.svg'}]],
    cleanUrls: true,
    lastUpdated: true,

    themeConfig: {
      logo: {src: '/handshake.svg', width: 24, height: 24},
        nav: [
            {text: 'Docs', link: '/what-is-negotiator'},
            {
                text: 'BBMRI-ERIC', items: [
                    {
                        text: 'Web',
                        link: 'https://bbmri-eric.eu',
                    },
                    {
                        text: 'Acceptance',
                        link: 'https://negotiator.acc.bbmri-eric.eu'
                    },
                    {
                        text: 'Production',
                        link: 'https://negotiator.bbmri-eric.eu'
                    }
                ]
            },
            {
                text: latestVersion,
                items: [
                    {
                        text: 'Releases',
                        link: 'https://github.com/BBMRI-ERIC/negotiator/releases',
                    }
                ]
            }
        ],
        search: {
            provider: 'local'
        },
        editLink: {
            pattern: 'https://github.com/bbmri-eric/negotiator/edit/main/docs/:path',
            text: 'Edit this page on GitHub'
        },
        sidebar: [
            {
                text: 'Introduction',
                collapsed: false,
                items: [
                    {text: 'What is Negotiator?', link: '/what-is-negotiator'},
                ]
            },
            {
                text: 'Users',
                collapsed: false,
                items: [
                    {text: 'Administrator', link: '/administrator'},
                    {text: 'Representative', link: '/representative'},
                ]
            },
            {
                text: 'Developers',
                collapsed: false,
                items: [
                    {text: 'Connecting to a Negotiator', link: 'connecting-to-negotiator'},
                ]
            }
        ],
        footer: {
            message: 'Released under the AGPL-3.0 License.',
            copyright: 'Copyright© 2019-present BBMRI-ERIC'
        },
        socialLinks: [
            {icon: 'github', link: 'https://github.com/BBMRI-ERIC/negotiator'}
        ]
    }
})
