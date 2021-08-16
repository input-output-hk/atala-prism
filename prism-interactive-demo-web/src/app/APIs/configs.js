// const getConfig = () => {
//   if (typeof window === 'undefined') return {};

//   const {
//     REACT_APP_ISSUER_ID,
//     REACT_APP_GRPC_CLIENT,
//     REACT_APP_ISSUER_NAME,
//     REACT_APP_ISSUER_DID,
//     REACT_APP_MAILCHIMP_URL,
//     REACT_APP_MAILCHIMP_U,
//     REACT_APP_MAILCHIMP_ID
//   } = window._env_;

//   return {
//     issuerId: window?.localStorage?.getItem('issuerId') || REACT_APP_ISSUER_ID,
//     issuerName: window?.localStorage?.getItem('issuerName') || REACT_APP_ISSUER_NAME,
//     issuerDid: window?.localStorage?.getItem('issuerDid') || REACT_APP_ISSUER_DID,
//     grpcClient: window?.localStorage?.getItem('backendUrl') || REACT_APP_GRPC_CLIENT,
//     mailchimpURL: window?.localStorage?.getItem('mailchimpURL') || REACT_APP_MAILCHIMP_URL,
//     mailchimpU: window?.localStorage?.getItem('mailchimpU') || REACT_APP_MAILCHIMP_U,
//     mailchimpID: window?.localStorage?.getItem('mailchimpID') || REACT_APP_MAILCHIMP_ID
//   };
// };

// export const config = getConfig();

export const config = {
  grpcClient: 'https://www.atalaprism.io:4433',
  issuerId: '091d41cc-e8fc-4c44-9bd3-c938dcf76dff',
  issuerName: 'Department of Interior, Replublic of Redland',
  issuerDid: 'did:iohk:test',
  mailchimpURL: '//iohk.us18.list-manage.com',
  mailchimpU: '8b77ede3540bcd2a9ee5160ef',
  mailchimpID: '25a1e6f305',
  REACT_APP_FIREBASE_CONFIG:
    '{"apiKey":"AIzaSyALQCJ2Qln5Js0yhW7OlNOBzlmW9J3CbyM","authDomain":"atala-test.firebaseapp.com","databaseURL":"https://atala-test.firebaseio.com","projectId":"atala-test","storageBucket":"atala-test.appspot.com","messagingSenderId":"777420890876","appId":"1:777420890876:web:5a0edf5a66b71df2772d79","measurementId":"G-8HRJEC3YJL"}'
};
