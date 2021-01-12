import { useTranslation } from 'react-i18next';

export const useTranslationWithPrefix = prefix => {
  const { t } = useTranslation();

  const tp = chain => t(`${prefix}.${chain}`);

  return tp;
};
