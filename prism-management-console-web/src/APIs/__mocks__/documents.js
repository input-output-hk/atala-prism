import { lorem } from 'faker';

const PARAGRAPH_COUNT = 30;

export const getTermsAndConditions = () => Promise.resolve(lorem.paragraphs(PARAGRAPH_COUNT));

export const getPrivacyPolicy = () => Promise.resolve(lorem.paragraphs(PARAGRAPH_COUNT));
