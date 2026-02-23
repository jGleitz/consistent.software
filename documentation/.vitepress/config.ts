import { defineConfig } from "vitepress"
import markdownItDeflist from "markdown-it-deflist"

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "consistent.software",
  description: "Strong modelling.",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: "Home", link: "/" },
      { text: "Examples", link: "/markdown-examples" },
    ],

    sidebar: [
      {
        text: "Examples",
        items: [
          { text: "Markdown Examples", link: "/markdown-examples" },
          { text: "Runtime API Examples", link: "/api-examples" },
        ],
      },
    ],

    socialLinks: [
      {
        icon: "github",
        link: "https://github.com/jGleitz/consistent.software",
      },
    ],

    search: {
      provider: "local",
    },
  },
  cleanUrls: true,
  markdown: {
    math: true,
    config(md) {
      md.use(markdownItDeflist)
    },
  },
  outDir: "./build/site",
  cacheDir: "./build/vitepress/cache",
  srcDir: "./src/main/markdown",
})
