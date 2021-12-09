const credentialsData = [
  {
    attribute1: 'attribute 1 value',
    attribute2: 1234
  },
  {
    attribute1: 'attribute 1 value',
    attribute2: 1234,
    extraAttribute: 'extra attribute value'
  }
];

const genericTemplates = [
  // first template
  `<h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    {{attribute1}}
  </h3>
  <h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    {{attribute2}}
  </h3>`,
  // second template
  `
  <h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    {{attribute1}}
  </h3>
  <div>
    {{attribute2}}
  </div>
  <div>
    {{doNotReplace}}
  </div>`
];

const expectedAfterReplace = [
  // first html
  `<h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    attribute 1 value
  </h3>
  <h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    1234
  </h3>`,
  // second html
  `
  <h3 style="color: #000000; font-size: 13px; font-weight: 600; word-break: break-all;">
    attribute 1 value
  </h3>
  <div>
    1234
  </div>
  <div>
    {{doNotReplace}}
  </div>`
];

export const testTemplates = genericTemplates.map((html, index) => ({
  rawTemplate: html,
  credentialData: credentialsData[index],
  expectedAfterReplace: expectedAfterReplace[index]
}));
