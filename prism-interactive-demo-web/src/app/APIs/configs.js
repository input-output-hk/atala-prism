let grpcClient;

if (process.env.NODE_ENV === 'production') {
  if (process.env.GATSBY_BRANCH) {
    grpcClient = `https://${process.env.GATSBY_BRANCH}.atalaprism.io:4433`;
  } else {
    grpcClient = 'https://www.atalaprism.io:4433';
  }
} else if (process.env.NODE_ENV === 'development') {
  grpcClient = 'http://localhost:10000';
} else {
  throw new Error('process.env.NODE_ENV is neither production nor development');
}

console.log('grpcClient: ', grpcClient);

export const config = {
  grpcClient,
  issuerId: '091d41cc-e8fc-4c44-9bd3-c938dcf76dff',
  issuerName: 'Department of Interior, Replublic of Redland',
  issuerDid: 'did:iohk:test',
  mailchimpURL: '//iohk.us18.list-manage.com',
  mailchimpU: '8b77ede3540bcd2a9ee5160ef',
  mailchimpID: '25a1e6f305',
  REACT_APP_FIREBASE_CONFIG:
    '{"apiKey":"AIzaSyALQCJ2Qln5Js0yhW7OlNOBzlmW9J3CbyM","authDomain":"atala-test.firebaseapp.com","databaseURL":"https://atala-test.firebaseio.com","projectId":"atala-test","storageBucket":"atala-test.appspot.com","messagingSenderId":"777420890876","appId":"1:777420890876:web:5a0edf5a66b71df2772d79","measurementId":"G-8HRJEC3YJL"}'
};
