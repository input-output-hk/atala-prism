import {
  string,
  shape,
  number,
  oneOf,
  oneOfType,
  func,
  object,
  bool,
  arrayOf,
  element,
  objectOf
} from 'prop-types';
import __ from 'lodash';
import { CONNECTION_STATUSES } from './constants';

export const connectionStatusesShape = __.values(CONNECTION_STATUSES);
export const connectionStatusesKeysShape = __.keys(CONNECTION_STATUSES);

export const contactShape = {
  avatar: string,
  name: string,
  identityNumber: number,
  admissionDate: number,
  email: string,
  status: oneOf(connectionStatusesShape),
  id: string
};

export const groupShape = {
  icon: string,
  courseName: string,
  courseId: string,
  certificate: shape({
    certificateName: string,
    certificateId: string
  }),
  credential: shape({
    credentialName: string,
    credentialId: string
  }),
  lastUpdate: number
};

export const credentialSummaryShape = {
  id: string,
  user: shape({
    icon: string,
    name: string
  }),
  date: number,
  totalCredentials: number
};

export const paymentShape = {
  id: string,
  // Not returning from the backend
  user: shape({
    avatar: string,
    name: string
  }),
  date: number,
  amount: number,
  // Not returning from the backend
  currency: string,
  status: string,
  failureReason: string
};

export const refShape = oneOfType([
  func,
  shape({
    current: object
  })
]);

export const dateObjectShape = {
  day: number,
  month: number,
  year: number
};

export const studentShape = {
  admissiondate: dateObjectShape,
  connectionid: string,
  connectionstatus: oneOf(connectionStatusesShape),
  connectiontoken: string,
  fullname: string,
  id: string,
  issuerid: string,
  universityassignedid: string
};

export const infiniteTableProps = {
  loading: bool.isRequired,
  getMoreData: func.isRequired,
  hasMore: bool.isRequired
};

export const credentialShape = {
  icon: string,
  name: string,
  identityNumber: number,
  admissionDate: number,
  email: string,
  status: oneOf(['PENDING_CONNECTION', 'CONNECTED']),
  id: string
};

export const credentialTabShape = {
  fetchCredentials: func.isRequired,
  tableProps: shape({
    credentials: arrayOf(shape()),
    selectionType: shape({
      selectedRowKeys: arrayOf(string)
    }),
    hasMore: bool,
    searching: bool
  }).isRequired,
  bulkActionsProps: shape({
    signSelectedCredentials: func,
    sendSelectedCredentials: func,
    toggleSelectAll: func,
    selectAll: bool,
    indeterminateSelectAll: bool
  }).isRequired,
  showEmpty: bool,
  initialLoading: bool
};

export const credentialTypeShape = {
  id: number,
  enabled: bool,
  isMultiRow: bool,
  name: string,
  logo: element,
  sampleImage: element,
  fields: arrayOf(
    shape({
      key: string,
      type: string,
      validations: arrayOf(string),
      isRowField: bool
    })
  ),
  placeholders: objectOf(string)
};

export const credentialTypesShape = {
  governmentId: shape(credentialTypeShape),
  educational: shape(credentialTypeShape),
  proofOfEmployment: shape(credentialTypeShape),
  healthIsurance: shape(credentialTypeShape)
};
