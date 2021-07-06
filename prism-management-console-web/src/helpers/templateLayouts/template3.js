export const template3 = {
  // FIXME: @ana-alleva
  header: `
  <style>
    .attribute-container div:nth-child(1) {
      width: 100% !important;
      background-color: red;
    }
  </style>
  <head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  </head>

  <body
  style="margin: auto; padding: 1em; width: 90vw; overflow-x: hidden; font-variant: normal; font-family:Arial, Helvetica, sans-serif; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;">
  <div
    style="max-width: 400px; border-radius: 10px; display: flex; flex-wrap: wrap; box-shadow: 0 1px 6px 0 rgba(32, 33, 36, .28);  align-items: center;">
    <!-- Inside this div you can change the background color of the header -->
    <div
      style="background-color: {{themeColor}}; display: flex; border-radius: 10px 10px 0 0; height: 100px; box-sizing: border-box; width: 100%; align-items: center; padding: 1em 2em">
      <!-- Here you can change the header info -->
      <div
        style="display: flex; flex-direction: column; width: 100%; box-sizing: border-box; border-radius: 10px 10px 0 0;">
        <!-- Small Text -->
        <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">{{credentialTitle}}</p>
        <!-- Big Text -->
        <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
          {{credentialSubtitle}}
        </h3>
      </div>
      <div style="width: 60px;">
        <!-- here you can change the credential image -->
        <img src={{image0}} />
      </div>
    </div>
    <div style="background-color: {{backgroundColor}}; width: 100%; display: flex; flex-wrap: wrap; padding: 1em 2em;">
      <div class="attribute-container" style="width: 100%; box-sizing: border-box; display: flex; :first-child: color">
        {{#attributes}}
      </div>
    </div>
  </div>
  </body>
`,
  body: `
  <div class="attribute" style="width: 50%;">
    <!-- Small Text -->
    <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
      {{attributeLabel}}</p>
    <!-- Big Text -->
    <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
      {{{{attributeLabel}}}}
    </h3>
  </div>
`
};
