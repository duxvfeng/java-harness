#!/usr/bin/env node

const fs = require("fs");

function readOption(args, name) {
  const index = args.indexOf(name);
  if (index === -1 || index + 1 >= args.length) {
    throw new Error(`Missing option: ${name}`);
  }
  return args[index + 1];
}

function lookup(name, context, root) {
  const parts = name.split(".");
  let value = context;
  for (const part of parts) {
    if (value !== null && value !== undefined && Object.prototype.hasOwnProperty.call(value, part)) {
      value = value[part];
    } else if (root !== context && root !== null && root !== undefined
        && Object.prototype.hasOwnProperty.call(root, part)) {
      value = root[part];
    } else {
      return undefined;
    }
  }
  return value;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function renderTemplate(template, context, root) {
  const sectionPattern = /\{\{#([A-Za-z0-9_.-]+)\}\}([\s\S]*?)\{\{\/\1\}\}/g;
  const withSections = template.replace(sectionPattern, (match, name, body) => {
    const value = lookup(name, context, root);
    if (Array.isArray(value)) {
      return value.map(item => renderTemplate(body, item, root)).join("");
    }
    if (value && typeof value === "object") {
      return renderTemplate(body, value, root);
    }
    return value ? renderTemplate(body, context, root) : "";
  });
  return withSections.replace(/\{\{([A-Za-z0-9_.-]+)\}\}/g, (match, name) => {
    const value = lookup(name, context, root);
    return value === undefined || value === null ? "" : escapeHtml(value);
  });
}

const args = process.argv.slice(2);
const templatePath = readOption(args, "--template");
const dataPath = readOption(args, "--data");
const outputPath = readOption(args, "--out");
const template = fs.readFileSync(templatePath, "utf8");
const data = JSON.parse(fs.readFileSync(dataPath, "utf8").replace(/^\uFEFF/, ""));
fs.writeFileSync(outputPath, renderTemplate(template, data, data), "utf8");
