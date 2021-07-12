export const template3 = {
  header: `
    <style>
      .attribute-container div:nth-child(1) {
        width: 100% !important;
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
            <p style="font-size: 9px; color: {{contrastThemeColor}}; margin: 0.53em 0 1.5em; text-transform: uppercase;">{{credentialTitle}}</p>
            <!-- Big Text -->
            <h3 style="color: {{contrastThemeColor}}; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
              {{credentialSubtitle}}
            </h3>
          </div>
          <div style="width: 60px;">
            <!-- here you can change the credential image -->
            <img src={{image0}} style="width: 44px;" />
          </div>
        </div>
        <div style="background-color: {{backgroundColor}}; width: 100%; display: flex; flex-wrap: wrap; padding: 1em 2em;
          border-radius: 0 0 10px 10px; box-shadow: 0 1px 6px 0 rgba(32, 33, 36, .28);">
          <div class="attribute-container" 
            style="width: 100%; box-sizing: border-box; display: flex; flex-wrap: wrap;" >
            {{#attributes}}
          </div>
        </div>
      </div>
    </body>`,
  dynamicAttribute: `
  <div class="attribute" style="width: 50%; width: calc(50% - 1em); margin: 0.5em;">
    <!-- Small Text -->
    <p style="font-size: 9px; color: {{contrastBackgroundColor}}; margin: 0.53em 0 1.5em; text-transform: uppercase; word-break: break-all;">
      {{attributeLabel}}</p>
    <!-- Big Text -->
    <h3 style="color: {{contrastBackgroundColor}}; font-size: 13px; font-weight: 600; word-break: break-all;">
      {{{{attributeLabelPlaceholder}}}}
    </h3>
  </div>`,
  fixedText: `
  <div class="attribute" style="width: 50%; width: calc(50% - 1em); margin: 0.5em;
  display: flex; align-items: flex-end; justify-content: flex-start;">
    <!-- Big Text -->
    <h3 style="color: {{contrastBackgroundColor}}; font-size: 13px; font-weight: 600; word-break: break-all;">
      {{text}}
    </h3>
  </div>`
};
