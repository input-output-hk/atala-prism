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

export const PAGE = 'page';
export const LANDING_PAGE = 'landing';
export const CREDENTIALS_PAGE = 'credentials';

export const VISION_NAME = 'vision';
export const DEMO_NAME = 'demo';
export const COMPONENTS_NAME = 'components';
export const BENEFITS_NAME = 'benefits';
export const USE_CASES_NAME = 'useCases';
export const CASE_STUDY_NAME = 'caseStudy';
export const GET_STARTED_NAME = 'getStarted';
export const FAQ_NAME = 'faq';
export const CONTACT_US_NAME = 'contactUs';
export const BLOG_NAME = 'blog';
export const RESOURCES_NAME = 'resources';
export const PIONEERS_NAME = 'pioneers';

export const APP_STORE_URL = 'https://apps.apple.com/app/atala-prism/id1515523675';
export const GOOGLE_PLAY_STORE_URL = 'https://play.google.com/store/apps/details?id=io.iohk.cvp';

// EXAMPLE CERTIFICATE CARD VALUES
export const CARD_UNIVERSITY_TITLE = 'Bachelor of Science';
export const CARD_UNIVERSITY_UNIVERSITY = 'University of Innovation and Technology';
export const CARD_UNIVERSITY_AWARD = 'First-class honors';
export const CARD_EMPLOYMENT_COMPANY = 'Decentralized Inc.';
export const CARD_EMPLOYMENT_ADDRESS = '67 Clasper Way, Herefoot, HF1 0AF';
export const CARD_EMPLOYMENT_STATUS = 'Full-time';
export const CARD_INSURANCE_PROVIDER = 'Verified Insurance Ltd.';
export const CARD_INSURANCE_CLASS = 'Health Insurance';
export const CARD_INSURANCE_POLICY_NUMBER = 'ABC-123456789';

export const GET_CREDENTIALS_EVENT = 'Get_Credentials';
export const SUPPORT_EVENT = 'support';
export const RESET_DEMO_EVENT = 'reset_demo';
export const CONTACT_US_EVENT = 'contact_us';
export const STEP_1_EVENT = 'step_1_event';
export const STEP_2_EVENT = 'step_2_event';
export const STEP_3_EVENT = 'step_3_event';
export const STEP_4_EVENT = 'step_4_event';
export const BLOG_EVENT = 'blog';
export const BLOG_POST_EVENT = 'blog_post_';
export const RESOURCES_EVENT = 'resources_menu';
export const PIONEERS_EVENT = 'pioneers';
