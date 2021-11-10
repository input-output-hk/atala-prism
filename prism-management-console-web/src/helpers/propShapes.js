import {
  string,
  shape,
  number,
  oneOf,
  oneOfType,
  func,
  bool,
  arrayOf,
  element,
  objectOf,
  instanceOf,
  node
} from 'prop-types';
import __ from 'lodash';
import {
  CONNECTION_STATUSES,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA,
  DESIGN_TEMPLATE,
  TEMPLATE_NAME_ICON_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from './constants';

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
    current: instanceOf(Element)
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

export const credentialTypeDetailsShape = shape({
  id: string,
  logo: string.isRequired,
  name: string.isRequired,
  state: number
});

export const credentialDataShape = shape({
  contactId: string,
  contactName: string,
  credentialTypeDetails: credentialTypeDetailsShape.isRequired,
  html: string
});

export const credentialShape = shape({
  contactId: string,
  credentialData: credentialDataShape.isRequired,
  credentialId: string.isRequired,
  credentialString: string.isRequired,
  encodedSignedCredential: string,
  externalId: string.isRequired,
  connectionStatus: oneOf(Object.values(CONNECTION_STATUSES))
});

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

export const credentialTypeShape = shape({
  id: string,
  enabled: bool,
  isMultiRow: bool,
  name: string,
  logo: string,
  sampleImage: oneOfType([element, string]),
  fields: arrayOf(
    shape({
      key: string,
      type: string,
      validations: arrayOf(string),
      isRowField: bool
    })
  ),
  placeholders: objectOf(string),
  template: string
});

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
  id: string.isRequired,
  name: string.isRequired,
  logo: string,
  sampleImage: string,
  state: number.isRequired
});

export const credentialTypesManagerShape = shape({
  getCredentialTypes: func,
  getCredentialTypeDetails: func,
  getTemplateCategories: func,
  createCategory: func
});

export const refPropShape = oneOfType([func, shape({ current: instanceOf(Element) })]);

export const antdV4FormShape = shape({ validateFields: func, resetFields: func });

export const templateBodyAttributeShape = shape({
  key: number,
  name: number,
  fieldKey: number,
  textAttributeIndex: number,
  dynamicAttributeIndex: number
});

export const importUseCasePropType = oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]);

export const templateCreationStepShape = oneOf([
  TEMPLATE_NAME_ICON_CATEGORY,
  DESIGN_TEMPLATE,
  TEMPLATE_CREATION_RESULT
]);

export const templateFiltersShape = {
  name: string,
  category: string,
  lastEdited: string,
  setName: func,
  setCategory: func,
  setLastEdited: func
};

export const templateSortingShape = {
  sortingBy: string,
  setSortingBy: func
};

export const checkboxPropShape = shape({
  checked: bool.isRequired,
  indeterminate: bool,
  disabled: bool,
  onChange: func.isRequired
});

export const emptyPropsShape = shape({
  photoSrc: string,
  model: string,
  isFilter: bool,
  button: node
});
