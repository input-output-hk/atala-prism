// eslint-disable-next-line camelcase
const DEFAUlT_PAGE_SIZE = 100;
// eslint-disable-next-line camelcase
export const HOLDER_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const GROUP_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CREDENTIAL_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const CREDENTIAL_SUMMARY_PAGE_SIZE = DEFAUlT_PAGE_SIZE;
// eslint-disable-next-line camelcase
export const PAYMENT_PAGE_SIZE = 20;
// eslint-disable-next-line camelcase
export const HARDCODED_LIMIT = 1000;

export const xScroll = 1300;
export const yScroll = 600;
export const drawerWidth = 450;
export const AVATAR_WIDTH = 50;

export const RIGHT = 'right';
export const LEFT = 'left';

const JPEG = 'image/jpeg';
const PNG = 'image/png';
export const ALLOWED_TYPES = [JPEG, PNG];
export const MAX_FILE_SIZE = 1073741824; // 1 Gb as maximum just so all my images can pass

// Local storage item names
export const USER_ROLE = 'userRole';
export const ORGANISATION_NAME = 'organisationName';
export const LOGO = 'logo';

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

export const CREDENTIAL_TYPES = {
  0: 'GOVERNMENT_ISSUED_DIGITAL_IDENTITY',
  1: 'UNIVERSITY_DEGREE',
  2: 'PROOF_OF_EMPLOYMENT',
  3: 'INSURANCE_POLICY'
};

export const GOVERNMENT_ISSUED_DIGITAL_IDENTITY = 0;
export const UNIVERSITY_DEGREE = 1;
export const PROOF_OF_EMPLOYMENT = 2;
export const INSURANCE_POLICY = 3;

export const SUBJECT_STATUSES = {
  0: 'UNCONNECTED',
  1: 'CONNECTED',
  2: 'CREDENTIAL_AVAILABLE',
  3: 'CREDENTIAL_SENT',
  4: 'CREDENTIAL_RECEIVED'
};

export const CONNECTED = 1;
export const CREDENTIAL_SENT = 3;

export const USER = 'atala-demo-web-user';

export const FEATURE_NAME = 'features';
export const CREDENTIAL_NAME = 'credentials';
export const DOWNLOAD_NAME = 'download';

export const APP_STORE_URL = 'https://www.apple.com/la/ios/app-store/';
export const GOOGLE_PLAY_STORE_URL = 'https://play.google.com/store';

// EXAMPLE CERTIFICATE CARD VALUES
export const CARD_UNIVERSITY_TITLE = 'Bachelor of Science';
export const CARD_UNIVERSITY_UNIVERSITY = 'Air Side University';
export const CARD_UNIVERSITY_AWARD = 'Upper second class honours';
export const CARD_EMPLOYMENT_COMPANY = 'Atala Inc.';
export const CARD_EMPLOYMENT_ADDRESS = '67 Clasper Way, Herefoot, HF1 0AF';
export const CARD_EMPLOYMENT_STATUS = 'Full Time';
export const CARD_INSURANCE_PROVIDER = 'Atala Insurance Ltd.';
export const CARD_INSURANCE_CLASS = 'Life Insurance';
export const CARD_INSURANCE_POLICY_NUMBER = 'ABC-123456789';
