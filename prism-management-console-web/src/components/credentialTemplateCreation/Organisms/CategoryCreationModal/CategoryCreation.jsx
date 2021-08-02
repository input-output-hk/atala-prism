import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useTemplateCategories } from '../../../../hooks/useCredentialTypes';
import { withApi } from '../../../providers/withApi';
import { antdV4FormShape, credentialTypesManagerShape } from '../../../../helpers/propShapes';
import CategoryNameInput from '../../Molecules/CategoryCreationModal/CategoryNameInput';
import CategoryIconSelector from '../../Molecules/CategoryCreationModal/CategoryIconSelector';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const CategoryCreation = ({ api, categoryForm, close }) => {
  const { t } = useTranslation();
  const { getTemplateCategories } = useTemplateCategories(api.credentialTypesManager);
  const [isLoading, setIsLoading] = useState(false);

  const validateNewCategory = () =>
    categoryForm.validateFields().catch(({ errorFields, values }) => ({
      errors: errorFields.map(errorField => errorField.errors),
      values
    }));

  const displayErrors = errors => errors.map(msg => message.error(t(msg)));

  const handleCategorySubmit = async () => {
    setIsLoading(true);
    const { errors, ...values } = await validateNewCategory();
    const isPartiallyValid = !errors?.length;
    if (!isPartiallyValid) {
      displayErrors(errors);
    } else {
      await api.credentialTypesManager.createCategory(values);
      close();
    }
    setIsLoading(false);
  };

  return (
    <>
      <CategoryNameInput getTemplateCategories={getTemplateCategories} />
      <CategoryIconSelector />
      <div className="buttonSection">
        <CustomButton
          className="theme-secondary"
          buttonText={t(`${i18nPrefix}.categoryCreationModal.save`)}
          loading={isLoading}
          buttonProps={{
            onClick: handleCategorySubmit
          }}
        />
      </div>
    </>
  );
};

CategoryCreation.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: credentialTypesManagerShape.isRequired
  }).isRequired,
  categoryForm: antdV4FormShape.isRequired,
  close: PropTypes.func.isRequired
};

export default withApi(CategoryCreation);
