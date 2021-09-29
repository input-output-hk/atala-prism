import _ from 'lodash';
import embeddedCompanyLogo from '../images/templates/genericCompanyLogo.svg';
import embeddedUserIcon from '../images/templates/genericUserIcon.svg';

const getNextIndexByKey = (array, key) => _.maxBy(array, item => item[key])?.[key] + 1 || 0;

export const getNewDynamicAttribute = attributes => {
  const index = attributes.length;
  const dynamicAttributeIndex = getNextIndexByKey(attributes, 'dynamicAttributeIndex');
  return {
    attributeLabel: `Attribute ${dynamicAttributeIndex + 1}`,
    attributeType: undefined,
    dynamicAttributeIndex,
    key: index
  };
};

export const getNewFixedTextAttribute = attributes => {
  const index = attributes.length;
  const textAttributeIndex = getNextIndexByKey(attributes, 'textAttributeIndex');
  return {
    text: `Text Field ${textAttributeIndex + 1}`,
    isFixedText: true,
    textAttributeIndex,
    key: index
  };
};

const getDefaultAttribute = index => ({
  attributeLabel: `Attribute ${index + 1}`,
  attributeType: undefined,
  dynamicAttributeIndex: index,
  key: index
});

const defaultCredentialBodyLength = 2;

export const defaultTemplateSketch = {
  name: '',
  category: '1',
  layout: 0,
  themeColor: '#D8D8D8',
  backgroundColor: '#FFFFFF',
  embeddedCompanyLogo,
  embeddedUserIcon,
  credentialTitle: 'Title',
  credentialSubtitle: 'Subtitle',
  credentialBody: new Array(defaultCredentialBodyLength)
    .fill(undefined)
    .map((_item, index) => getDefaultAttribute(index))
};

export const insertFormChangeIntoArray = (change, oldArray) => {
  const changeArray = Array.from(change); // casting to array because it's a sparse array

  // change event is adding / deleting an item or a change in sort
  const isArrayChange = changeArray.every(item => Boolean(item?.key) || item?.key === 0);
  if (isArrayChange) return changeArray;

  // change event is updating an item's attribute
  // an empty array as first parameter is needed to prevent mutating the `oldArray` directly
  const newPartialArray = _.merge([], oldArray, changeArray);

  const oldArrayTail = oldArray.slice(newPartialArray.length);
  const newArray = newPartialArray.concat(oldArrayTail);
  return newArray;
};
