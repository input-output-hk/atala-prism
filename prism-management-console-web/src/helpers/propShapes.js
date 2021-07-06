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
  objectOf,
  instanceOf
} from 'prop-types';
import __ from 'lodash';
import { CONNECTION_STATUSES } from './constants';

export const connectionStatusesShape = __.values(CONNECTION_STATUSES);
export const connectionStatusesKeysShape = __.keys(CONNECTION_STATUSES);

export const contactCreationShape = {
  contactName: string,
  externalId: string,
  key: number.isRequired
};

export const contactShape = {
  contactName: string,
  externalId: string,
  contactId: string,
  status: oneOf(connectionStatusesShape)
};

export const groupShape = {
  groupid: string,
  name: string
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
    searching: bool,
    sortingProps: shape({
      sortingBy: string,
      setSortingBy: func,
      sortDirection: string,
      setSortDirection: func
    }).isRequired
  }).isRequired,
  bulkActionsProps: shape({
    signSelectedCredentials: func,
    sendSelectedCredentials: func,
    toggleSelectAll: func,
    selectAll: bool,
    indeterminateSelectAll: bool
  }).isRequired,
  showEmpty: bool,
  initialLoading: bool,
  searchDueGeneralScroll: bool
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

export const credentialTypesShape = arrayOf(credentialTypeShape);

export const columnShape = arrayOf({
  label: string,
  fieldKey: string,
  width: oneOf([number, string])
});

export const skeletonShape = arrayOf({
  name: string,
  placeholder: string,
  fieldKey: string,
  rules: arrayOf(shape({ message: string }))
});

export const templateCategoryShape = shape({
  id: number.isRequired,
  name: string.isRequired,
  logo: string,
  sampleImage: string,
  state: number.isRequired
});

export const credentialTypesManagerShape = shape({
  getCredentialTypes: func,
  getCredentialTypeDetails: func,
  getTemplateCategories: func
});

export const refPropShape = oneOfType([func, shape({ current: instanceOf(Element) })]);

export const antdV4FormShape = shape({ validateFields: func, resetFields: func });

export const templateBodyAttributeShape = shape({
  key: number,
  name: string,
  fieldKey: number,
  textAttributeIndex: number
});
