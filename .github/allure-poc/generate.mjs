// Temporary proof of concept: joins the test results of all operating systems into one Allure report.
import { execFileSync } from "node:child_process";
import { copyFileSync, globSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { basename, resolve } from "node:path";

const root = resolve("allure-work");
mkdirSync(`${root}/dumps`, { recursive: true });
const allure = resolve(".github/allure-poc/node_modules/.bin/allure");
const systems = ["Linux", "macOS", "Windows"];
const targets = { jvmTest: "JVM", jsNodeTest: "JS (Node)", jsBrowserTest: "JS (Browser)" };
const environments = Object.fromEntries(systems.map((os) => [os, { name: os, matcher: `@@${os}@@` }]));
const environmentsSource = JSON.stringify(environments).replace(/"@@(\w+)@@"/g, (_, os) => `({ labels }) => labels.some((l) => l.name === "os" && l.value === "${os}")`);

for (const os of systems) {
  for (const [task, target] of Object.entries(targets)) {
    const id = `${os}-${task}`;
    const input = `${root}/in/${id}`;
    mkdirSync(input, { recursive: true });
    for (const file of globSync(`results/test-results (${os})/**/build/test-results/${task}/*.xml`)) {
      const seen = new Map();
      const xml = readFileSync(file, "utf8").replace(/<testcase name="([^"]*)" classname="([^"]*)"/g, (_, name, className) => {
        const key = `${className}|${name}`;
        const count = (seen.get(key) ?? 0) + 1;
        seen.set(key, count);
        return `<testcase name="${name}" classname="${os}/${target}/${className}${count > 1 ? `#${count}` : ""}"`;
      });
      writeFileSync(`${input}/${basename(file)}`, xml);
    }
    const config = `${root}/${id}.mjs`;
    writeFileSync(config, `export default { name: "x", output: "${root}/o-${id}", dump: "${root}/dumps/${id}", environments: ${environmentsSource}, defaultLabels: { os: "${os}", target: "${target}" } };`);
    execFileSync(allure, ["generate", "--config", config, input], { stdio: "inherit" });
  }
}

const config = `${root}/final.mjs`;
writeFileSync(config, `export default { name: "Test Report", output: "${root}/report", environments: ${environmentsSource}, plugins: { awesome: { options: { singleFile: true, reportLanguage: "en", groupBy: ["target", "parentSuite", "suite"] } } } };`);
execFileSync(allure, ["generate", "--config", config, "--dump", `${root}/dumps/*.zip`], { stdio: "inherit" });
copyFileSync(`${root}/report/index.html`, "test-report-all-os.html");
