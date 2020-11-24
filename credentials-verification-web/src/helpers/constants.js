import __ from 'lodash';

// eslint-disable-next-line camelcase
export const BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS = 3000; // The allowed delay for the extension to inject it's sdk
// eslint-disable-next-line camelcase
export const BROWSER_WALLET_CHECK_INTERVAL_MS = 500; // Delay before checking if the extension has injected it's SDK
export const SEARCH_DELAY_MS = 1000;
// eslint-disable-next-line camelcase
const DEFAUlT_PAGE_SIZE = 10;
// eslint-disable-next-line camelcase
export const HOLDER_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CONTACT_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const MAX_CONTACTS = 10000;
// eslint-disable-next-line camelcase
export const MAX_CREDENTIALS = 10000;
// eslint-disable-next-line camelcase
export const GROUP_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CREDENTIAL_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const PAYMENT_PAGE_SIZE = DEFAUlT_PAGE_SIZE;

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

// Local storage item names
export const SESSION = 'session';
export const DEFAULT_LANGUAGE = 'defaultLanguage';

// Roles
export const VERIFIER = 'VERIFIER';
export const ISSUER = 'ISSUER';

// Wallet
export const LOADING = 'LOADING';
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

export const TABLE_HEIGHTS = {
  xs: 150,
  sm: 200,
  md: 280,
  lg: 360,
  xl: 460
};
// credentials tabs
export const CREDENTIALS_ISSUED = 'CREDENTIALS_ISSUED';
export const CREDENTIALS_RECEIVED = 'CREDENTIALS_RECEIVED';
