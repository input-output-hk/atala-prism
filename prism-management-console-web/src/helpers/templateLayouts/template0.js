export const template0 = `<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
</head>

<body
style="margin: auto; padding: 1em; width: 90vw; overflow-x: hidden; font-variant: normal; font-family:Arial, Helvetica, sans-serif; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;">
<div
    style="max-width: 400px; border-radius: 10px; display: flex; flex-wrap: wrap; box-shadow: 0 1px 6px 0 rgba(32, 33, 36, .28); border: 1px solid #e5e5e5;">
    <div style="position: relative; width: 100%;">
        <!-- Inside this div you can change the background color of the header -->
        <div
            style="background-color: {{themeColor}}; display: flex; flex-direction: column; width: 100%; padding: 1em 2em; box-sizing: border-box; border-radius: 10px 10px 0 0;">
            <!-- Here you can change the header info -->
            <!-- Small Text --> 
            <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">{{credentialTitle}}</p>
            <!-- Big Text -->
            <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
                {{credentialSubtitle}}
            </h3>
        </div>
    </div>
    <!-- Inside this div you can change the background color of the content -->
    <div style="background-color:{{backgroundColor}}; width: 100%; box-sizing: border-box; display: flex;">
        <div style="width: 80px; display: flex; padding: 0 0 0 1em; justify-content: center; align-items: center;">
            <!-- here you can change the credential image -->
            <img src={{image1}} />
        </div>
        <div style="width: 320px; display: flex; flex-wrap: wrap; padding: 1em;">
            <div style="width: 50%;">
                <!-- Small Text -->
                <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
                    Full name</p>
                <!-- Big Text -->
                <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
                    Title
                </h3>
            </div>
            <div style="width: 50%;">
                <!-- Small Text -->
                <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
                    Full name</p>
                <!-- Big Text -->
                <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
                    Title
                </h3>
            </div>
            <div
                style="width: 100%; border-bottom: 1px solid #e5e5e5; height: 1px; margin: 1em auto; box-sizing: border-box;">
            </div>
            <div style="width: 50%;">
                <!-- Small Text -->
                <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
                    Full name</p>
                <!-- Big Text -->
                <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
                    Title
                </h3>
            </div>
            <div style="width: 50%;">
                <!-- Small Text -->
                <p style="font-size: 9px; color: #828282; margin: 0.53em 0 1.5em; text-transform: uppercase;">
                    Full name</p>
                <!-- Big Text -->
                <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 600;">
                    Title
                </h3>
            </div>
        </div>
    </div>
</div>
</body>`;
