import { useTranslation } from 'react-i18next';

export const useTranslationWithPrefix = prefix => {
  const { t } = useTranslation();

  return chain => t(`${prefix}.${chain}`);
};
