import i18n from 'i18next';
import { IMPORT_CONTACTS } from '../constants';
import { expectValueNotExist, expectUniqueValue } from '../formRules';

// Must be a function for translation reasons
export const CONTACT_FORM_COLUMNS = () => [
  {
    label: i18n.t('contacts.table.columns.contactName'),
    fieldKey: 'contactName',
    width: 400
  },
  {
    label: i18n.t('contacts.table.columns.externalid'),
    fieldKey: 'externalid',
    width: 500
  },
  {
    label: i18n.t('contacts.table.columns.actions'),
    fieldKey: 'actions'
  }
];

export const CONTACT_FORM = (preexistingContacts, form) => [
  {
    name: 'contactName',
    placeholder: i18n.t('contacts.table.columns.contactName'),
    fieldKey: 'contactName',
    rules: [{ required: true, message: 'Contact Name is required.' }]
  },
  {
    name: 'externalid',
    placeholder: i18n.t('contacts.table.columns.externalid'),
    fieldKey: 'externalid',
    rules: [
      { required: true, message: 'External ID is required.' },
      {
        message: i18n.t('manualImport.table.uniqueFieldRequirement', {
          field: i18n.t('contacts.table.columns.externalid')
        }),
        validator: async (_, externalid, callback) => {
          const formValues = form.getFieldValue(IMPORT_CONTACTS);
          return expectUniqueValue(formValues, externalid, 'externalid', callback);
        }
      },
      {
        message: i18n.t('manualImport.table.checkPreexistingRequirement', {
          field: i18n.t('contacts.table.columns.externalid')
        }),
        validator: async (_, externalid, callback) =>
          expectValueNotExist(preexistingContacts, externalid, 'externalid', callback)
      }
    ]
  }
];

export const CONTACT_INITIAL_VALUE = [{ contactName: null, externalid: null }];
