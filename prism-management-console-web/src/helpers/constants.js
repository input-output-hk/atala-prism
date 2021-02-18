import __ from 'lodash';

// The allowed delay for the extension to inject it's sdk
export const BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS = 3000;
export const REQUEST_AUTH_TIMEOUT_MS = 9000; // Delay for heavy requests like sign credentials
export const TIMEOUT_MULTIPLIER_MS = 100; // Delay for fetching each entity

// Delay before checking if the extension has injected its SDK
export const BROWSER_WALLET_CHECK_INTERVAL_MS = 500;
export const SEARCH_DELAY_MS = 1000;
export const DEFAULT_PAGE_SIZE = 10;
export const CONTACT_PAGE_SIZE = DEFAULT_PAGE_SIZE;
export const GROUP_PAGE_SIZE = DEFAULT_PAGE_SIZE;
export const CREDENTIAL_PAGE_SIZE = DEFAULT_PAGE_SIZE;
export const PAYMENT_PAGE_SIZE = DEFAULT_PAGE_SIZE;
export const MAX_CONTACTS = 10000;
export const MAX_CREDENTIALS = 10000;

export const xScroll = 1300;
export const yScroll = 600;
export const drawerWidth = 450;
export const AVATAR_WIDTH = 50;

export const CONNECTED = 'CONNECTED';
export const PENDING_CONNECTION = 'PENDING_CONNECTION';

export const CONNECTION_STATUSES = {
  invitationMissing: 1,
  connectionMissing: 2,
  connectionAccepted: 3,
  connectionRevoked: 4
};

export const NORMALIZED_CONNECTION_STATUSES = [CONNECTED, PENDING_CONNECTION];

export const CONNECTION_STATUSES_TRANSLATOR = __.invert(CONNECTION_STATUSES);

export const INDIVIDUAL_STATUSES = {
  created: 1,
  invited: 2,
  connected: 3,
  revoked: 4
};
export const INDIVIDUAL_STATUSES_TRANSLATOR = __.invert(INDIVIDUAL_STATUSES);

export const CREDENTIAL_STATUSES = {
  credentialDraft: 1,
  credentialSigned: 2,
  credentialSent: 3,
  credentialRevoked: 4
};

export const VALID_CREDENTIAL_STATUSES = {
  credentialDraft: 1,
  credentialSigned: 2,
  credentialSent: 3
};

export const CREDENTIAL_STATUSES_TRANSLATOR = __.invert(CREDENTIAL_STATUSES);

// eslint-disable-next-line quotes
export const EXAMPLE_DEGREE_NAME = "Bachelor's in Engineering";
export const EXAMPLE_UNIVERSITY_NANE = 'Free University Tbilisi';
export const EXAMPLE_AWARD = 'First-Class Honours';
export const EXAMPLE_FULL_NAME = 'Student Name';
export const EXAMPLE_START_DATE = 1555005000;
export const EXAMPLE_GRADUATION_DATE = 1555005000;

// File constants
const JPEG = 'image/jpeg';
const PNG = 'image/png';
const ODS = 'application/vnd.oasis.opendocument.spreadsheet';
const XLS = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
const XLSX = 'application/vnd.ms-exce';
export const ALLOWED_IMAGE_TYPES = [JPEG, PNG];
export const ALLOWED_EXCEL_TYPES = [ODS, XLS, XLSX];
export const ALLOWED_TYPES = [...ALLOWED_EXCEL_TYPES, ...ALLOWED_IMAGE_TYPES];
export const MAX_FILE_SIZE = 1073741824; // 1 Gb as maximum just so all my images can pass
export const IMAGE = 'image';
export const EXCEL = 'excel';
export const ANY = 'any';
export const INVALID_TYPE = 'invalidType';
export const TOO_LARGE = 'tooLarge';
// default web encoding
export const ENCODING_UTF = 'UTF-8';
// default excel encoding
export const ENCODING_ISO = 'ISO-8859-1';

// Local storage item names
export const SESSION = 'session';
export const DEFAULT_LANGUAGE = 'defaultLanguage';

// Wallet
export const LOADING = 'LOADING';
export const CONFIRMED = 'CONFIRMED';
export const UNCONFIRMED = 'UNCONFIRMED';
export const UNLOCKED = 'UNLOCKED';
export const LOCKED = 'LOCKED';
export const MISSING_WALLET_ERROR = Error('errors.walletNotRunning');
export const WALLET_NOT_REGISTERED_ERROR = Error('errors.walletNotRegistered');

// Connection status
export const CONNECTION_ACCEPTED = 3;

// Import contacts methods
export const BULK_IMPORT = 'bulkImport';
export const MANUAL_IMPORT = 'manualImport';

// Bulk import steps
export const COMPLETE_SPREADSHEET_STEP = 0;
export const ASSIGN_TO_GROUPS = 1;

// Entities keys
export const CONTACT_NAME_KEY = 'contactName';
export const EXTERNAL_ID_KEY = 'externalid';
export const GROUP_NAME_KEY = 'name';

// Bulk import data
export const COMMON_CONTACT_HEADERS = [CONTACT_NAME_KEY, EXTERNAL_ID_KEY];
export const CONTACT_METADATA_KEYS = ['index', 'originalArray'];
export const COMMON_CREDENTIALS_HEADERS = [EXTERNAL_ID_KEY, CONTACT_NAME_KEY];
// Keys
export const ENTER = 'Enter';
export const ARROW_LEFT = 'ArrowLeft';
export const ARROW_RIGHT = 'ArrowRight';

// Import contact data use cases
export const IMPORT_CONTACTS = 'importContacts';
export const IMPORT_CREDENTIALS_DATA = 'importCredentialsData';

// Credentials action types
export const REVOKE_CREDENTIALS = 'revokeCredentials';
export const SIGN_CREDENTIALS = 'signCredentials';
export const SEND_CREDENTIALS = 'sendCredentials';
export const REVOKE_SINGLE_CREDENTIAL = 'revokeSingleCredential';
export const SIGN_SINGLE_CREDENTIAL = 'signSingleCredential';
export const SEND_SINGLE_CREDENTIAL = 'sendSingleCredential';

// Status badge use cases
export const CONTACT_STATUS = 'contacts';
export const CREDENTIAL_STATUS = 'credentials';

// credentials creation steps
export const NEW_CREDENTIALS_STEP_UNIT = 1;
export const SELECT_CREDENTIAL_TYPE_STEP = 0;
export const SELECT_RECIPIENTS_STEP = 1;
export const IMPORT_CREDENTIAL_DATA_STEP = 2;
export const PREVIEW_AND_SIGN_CREDENTIAL_STEP = 3;
export const NEW_CREDENTIAL_STEP_COUNT = 4;

// status flags
export const FAILED = 'failed';
export const SUCCESS = 'success';

// this format also allows: 'DD/MM/YYYY', 'DD-MM-YY' and 'DD/MM/YY'
export const DEFAULT_DATE_FORMAT = 'DD/MM/YYYY';
export const LONG_DATE_FORMAT = 'DD MMMM YYYY';

// credentials tabs
export const CREDENTIALS_ISSUED = 'CREDENTIALS_ISSUED';
export const CREDENTIALS_RECEIVED = 'CREDENTIALS_RECEIVED';

// groups
export const GROUP_NAME_STATES = {
  initial: null,
  loading: 'loading',
  possible: 'possible',
  failed: 'failed'
};

// error codes
export const UNKNOWN_DID_SUFFIX_ERROR_CODE = 2;
