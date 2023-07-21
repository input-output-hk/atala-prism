exports.handler = async (event, context, callback) => {

    const request = event.Records[0].cf.request;
    const headers = request.headers;

    const user = 'demo';
    const pass = 'iohk4ever';

    const basicAuthentication = 'Basic ' + new Buffer(user + ':' + pass).toString('base64');

    if (typeof headers.authorization == 'undefined' || headers.authorization[0].value != basicAuthentication) {
        const body = 'You are not authorized to enter';
        const response = {
            status: '401',
            statusDescription: 'Unauthorized',
            body: body,
            headers: {
                'www-authenticate': [{key: 'WWW-Authenticate', value:'Basic'}]
            },
        };
        callback(null, response);
    }
    callback(null, request);
};