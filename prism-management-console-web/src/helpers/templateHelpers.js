import embeddedCompanyLogo from '../images/templates/genericCompanyLogo.svg';
import embeddedUserIcon from '../images/templates/genericUserIcon.svg';

export const getNewDynamicAttribute = attributes => {
  const index = attributes.length;
  const dynamicAttributeIndex = attributes.filter(attr => !attr.isFixedText).length;
  return {
    attributeLabel: `Attribute ${dynamicAttributeIndex + 1}`,
    attributeType: undefined,
    dynamicAttributeIndex,
    key: index
  };
};

export const getNewFixedTextAttribute = attributes => {
  const index = attributes.length;
  const textAttributeIndex = attributes.filter(attr => attr.isFixedText).length;
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
  category: undefined,
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
