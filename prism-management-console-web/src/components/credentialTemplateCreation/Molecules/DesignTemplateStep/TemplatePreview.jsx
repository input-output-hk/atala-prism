import React from 'react';
import CredentialsViewer from '../../../newCredential/Molecules/CredentialsViewer/CredentialsViewer';

const credentialViews = [
  `<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
</head>

<body style="margin: auto; padding: 1em; width: 90vw; overflow-x: hidden; font-variant: normal; font-family:Arial, Helvetica, sans-serif; -webkit-font-smoothing: antialiased;
-moz-osx-font-smoothing: grayscale;">
  <div style="max-width: 400px; border: 1px solid #e5e5e5; border-radius: 10px; display: flex; flex-wrap: wrap; box-shadow: 0 1px 6px 0 rgba(32, 33, 36, .28);">
      <div style="display: flex; background: #f0f0f0; width: 100%; padding: 1.5em; border-radius: 10px 10px 0 0;">
          <div style="display: flex; flex-direction: column; width: 80%;">
              <p style="font-size: 10px; color: #828282; margin: 0.53em 0;">
                  National ID Card
              </p>
              <h1 style="font-size: 16px; margin: 0; color: #3c393a; width: 100%;">
                  {{issuerName}}
              </h1>
          </div>
          <div style="display: flex; width: 20%; align-items: center; justify-content: flex-end;">
              <img
                  src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzUiIGhlaWdodD0iMjMiIHZpZXdCb3g9IjAgMCAzNSAyMyIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTIuNTkzMjEgMEMxLjE2MDk4IDAgMCAxLjE2MDk4IDAgMi41OTMyMVYyMC4wOTI3QzAgMjEuNTI0OSAxLjE2MDk4IDIyLjY4NTkgMi41OTMyMSAyMi42ODU5SDExLjU0MTlWMEgyLjU5MzIxWiIgZmlsbD0iI0ZGNEI1NSIvPgo8cGF0aCBkPSJNMjMuMDg0NiAwLjAwMDI0NDE0MUgxMS41NDI1VjIyLjY4NjJIMjMuMDg0NlYwLjAwMDI0NDE0MVoiIGZpbGw9IndoaXRlIi8+CjxwYXRoIGQ9Ik0zMi4wMzIyIDBIMjMuMDgzNVYyMi42ODU4SDMyLjAzMjJDMzMuNDY0NCAyMi42ODU4IDM0LjYyNTQgMjEuNTI0OSAzNC42MjU0IDIwLjA5MjZWMi41OTMyMUMzNC42MjU0IDEuMTYwOTggMzMuNDY0NSAwIDMyLjAzMjIgMFoiIGZpbGw9IiNGRjRCNTUiLz4KPC9zdmc+Cg==' />
          </div>
      </div>
      <div style="display: flex; width: 100%; flex-wrap: wrap; padding: 1.5em 0;">
          <div style="width: 30%; display: flex; align-items: center; text-align: center; justify-content: center;">
              <img style="background: #ffeaeb; padding: 1em; border-radius: 10px;"
                  src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzEiIGhlaWdodD0iMzgiIHZpZXdCb3g9IjAgMCAzMSAzOCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTE1LjIwOTkgMTguNjAzMUMxOS42MTI4IDE4LjYwMzEgMjMuMTgyMyAxNC42Mjg5IDIzLjE4MjMgOS43MjY1N0MyMy4xODIzIDQuODI0MSAyMi4wMTA0IDAuODQ5OTc2IDE1LjIwOTkgMC44NDk5NzZDOC40MDk0NiAwLjg0OTk3NiA3LjIzNzMgNC44MjQxIDcuMjM3MyA5LjcyNjU3QzcuMjM3MyAxNC42Mjg5IDEwLjgwNjggMTguNjAzMSAxNS4yMDk5IDE4LjYwMzFaIiBmaWxsPSIjRkYyRDNCIi8+CjxwYXRoIGQ9Ik0wLjE1MTQ3IDMyLjE1NjVDMC4xNTAxMTIgMzEuODU3NiAwLjE0ODc1NSAzMi4wNzIzIDAuMTUxNDcgMzIuMTU2NVYzMi4xNTY1WiIgZmlsbD0iI0ZGMkQzQiIvPgo8cGF0aCBkPSJNMzAuMjY3NiAzMi4zOUMzMC4yNzE5IDMyLjMwODIgMzAuMjY5MSAzMS44MjIzIDMwLjI2NzYgMzIuMzlWMzIuMzlaIiBmaWxsPSIjRkYyRDNCIi8+CjxwYXRoIGQ9Ik0zMC4yNTA0IDMxLjc5OEMzMC4xMDI4IDIzLjI1OCAyOC44ODYgMjAuODI0NSAxOS41NzUyIDE5LjI4NDJDMTkuNTc1MiAxOS4yODQyIDE4LjI2NDUgMjAuODE1MSAxNS4yMDk3IDIwLjgxNTFDMTIuMTU0OCAyMC44MTUxIDEwLjg0MzkgMTkuMjg0MiAxMC44NDM5IDE5LjI4NDJDMS42MzQ2NyAyMC44MDc3IDAuMzQzOTM2IDIzLjIwNTEgMC4xNzQyMjIgMzEuNTIwNkMwLjE2MDMwNSAzMi4xOTk2IDAuMTUzODU2IDMyLjIzNTMgMC4xNTEzNjcgMzIuMTU2NUMwLjE1MTkzMyAzMi4zMDQyIDAuMTUyNjEyIDMyLjU3NzQgMC4xNTI2MTIgMzMuMDUzN0MwLjE1MjYxMiAzMy4wNTM3IDIuMzY5MzEgMzcuMTUgMTUuMjA5NyAzNy4xNUMyOC4wNDk4IDM3LjE1IDMwLjI2NjcgMzMuMDUzNyAzMC4yNjY3IDMzLjA1MzdDMzAuMjY2NyAzMi43NDc2IDMwLjI2NjkgMzIuNTM0OCAzMC4yNjczIDMyLjM5QzMwLjI2NDggMzIuNDM4OCAzMC4yNTk4IDMyLjM0NDMgMzAuMjUwNCAzMS43OThaIiBmaWxsPSIjRkYyRDNCIi8+Cjwvc3ZnPgo=' />
          </div>
          <div style="width: 70%; display: flex; flex-wrap: wrap;">
              <div style="width: 50%;">
                  <p style="font-size: 10px; color: #828282;">Identity Number</p>
                  <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 500;">
                      {{identityNumber}}
                  </h3>
              </div>
              <div style="width: 50%;">
                  <p style="font-size: 10px; color: #828282;">Date of Birth</p>
                  <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 500;">
                      {{dateOfBirth}}
                  </h3>
              </div>
              <div style="width: 50%;">
                  <p style="font-size: 10px; color: #828282;">Full Name</p>
                  <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 500;">
                      {{fullName}}
                  </h3>
              </div>
              <div style="width: 50%;">
                  <p style="font-size: 10px; color: #828282;">Expiration Date</p>
                  <h3 style="color: #3c393a; margin: -0.5em 0 0 0; font-size: 13px; font-weight: 500;">
                      {{expiryDate}}
                  </h3>
              </div>
          </div>
      </div>
  </div>
</body>`
];

const TemplatePreview = () => (
  <CredentialsViewer credentialViews={credentialViews} showBrowseControls={false} />
);

TemplatePreview.propTypes = {};

export default TemplatePreview;
