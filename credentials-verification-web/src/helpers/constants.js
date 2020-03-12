import __ from 'lodash';

// eslint-disable-next-line camelcase
const DEFAUlT_PAGE_SIZE = 10;
// eslint-disable-next-line camelcase
export const HOLDER_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const GROUP_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CREDENTIAL_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CREDENTIAL_SUMMARY_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const PAYMENT_PAGE_SIZE = DEFAUlT_PAGE_SIZE;

export const xScroll = 1300;
export const yScroll = 600;
export const drawerWidth = 450;
export const AVATAR_WIDTH = 50;

export const PENDING_CONNECTION = 'PENDING_CONNECTION';
export const CONNECTION_STATUSES = {
  invitationMissing: 0,
  connectionMissing: 1,
  connectionAccepted: 2,
  connectionRevoked: 3
};
export const CONNECTION_STATUSES_TRANSLATOR = __.invert(CONNECTION_STATUSES);

export const INDIVIDUAL_STATUSES = {
  created: 0,
  invited: 1,
  connected: 2,
  revoked: 3
};
export const INDIVIDUAL_STATUSES_TRANSLATOR = __.invert(INDIVIDUAL_STATUSES);

export const CONNECTED = 'CONNECTED';

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
export const USER_ROLE = 'userRole';
export const ORGANISATION_NAME = 'organisationName';
export const LOGO = 'logo';
export const DEFAULT_LANGUAGE = 'defaultLanguage';
export const USER_ID = 'userId';

// Roles
export const VERIFIER = 'VERIFIER';
export const ISSUER = 'ISSUER';

// Wallet
export const MISSING_WALLET_ERROR = 'Wallet cannot be Unlocked';
const MISSING = 'MISSING';
export const UNLOCKED = 'UNLOCKED';
const LOCKED = 'LOCKED';
const WalletStatuses = {
  0: MISSING,
  1: UNLOCKED,
  2: LOCKED
};

export const translateStatus = status => WalletStatuses[status];

// Connection status
export const CONNECTION_ACCEPTED = 2;

// Import connections steps
export const DOWNLOAD_STEP = 0;
export const UPLOAD_STEP = 1;

// Keys
export const ENTER = 'Enter';
export const ARROW_LEFT = 'ArrowLeft';
export const ARROW_RIGHT = 'ArrowRight';
