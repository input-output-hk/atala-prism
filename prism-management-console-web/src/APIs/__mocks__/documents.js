import { lorem } from 'faker';

export const getTermsAndConditions = () => Promise.resolve(lorem.paragraphs(30));

export const getPrivacyPolicy = () => Promise.resolve(lorem.paragraphs(30));
