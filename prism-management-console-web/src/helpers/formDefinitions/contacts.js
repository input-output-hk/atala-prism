import i18n from 'i18next';
import { IMPORT_CONTACTS } from '../constants';
import { expectValueNotExist, expectUniqueValue } from '../formRules';

// Must be a function for translation reasons
export const getContactFormColumns = () => [
  {
    label: i18n.t('contacts.table.columns.contactName'),
    fieldKey: 'contactName',
    width: 400
  },
  {
    label: i18n.t('contacts.table.columns.externalId'),
    fieldKey: 'externalId',
    width: 500
  },
  {
    label: i18n.t('contacts.table.columns.actions'),
    fieldKey: 'actions'
  }
];

export const getContactFormSkeleton = (preexistingContacts, form) => [
  {
    name: 'contactName',
    placeholder: i18n.t('contacts.table.columns.contactName'),
    fieldKey: 'contactName',
    rules: [{ required: true, message: 'Contact Name is required.' }],
    editable: true
  },
  {
    name: 'externalId',
    placeholder: i18n.t('contacts.table.columns.externalId'),
    fieldKey: 'externalId',
    rules: [
      { required: true, message: 'External ID is required.' },
      {
        message: i18n.t('manualImport.table.uniqueFieldRequirement', {
          field: i18n.t('contacts.table.columns.externalId')
        }),
        validator: async (_, externalId, callback) => {
          const formValues = form.getFieldValue(IMPORT_CONTACTS);
          return expectUniqueValue(formValues, externalId, 'externalId', callback);
        }
      },
      {
        message: i18n.t('manualImport.table.checkPreexistingRequirement', {
          field: i18n.t('contacts.table.columns.externalId')
        }),
        validator: async (_, externalId, callback) =>
          expectValueNotExist(preexistingContacts, externalId, 'externalId', callback)
      }
    ],
    editable: true
  }
];

export const CONTACT_INITIAL_VALUE = [{ contactName: null, externalId: null }];
