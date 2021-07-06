import { lorem } from 'faker';

export const getTermsAndConditions = () => new Promise(resolve => resolve(lorem.paragraphs(30)));

export const getPrivacyPolicy = () => new Promise(resolve => resolve(lorem.paragraphs(30)));
