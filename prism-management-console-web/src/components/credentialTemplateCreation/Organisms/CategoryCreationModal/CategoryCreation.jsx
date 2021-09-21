import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useTemplateCategories } from '../../../../hooks/useCredentialTypes';
import { withApi } from '../../../providers/withApi';
import {
  antdV4FormShape,
  credentialTypesManagerShape,
  templateCategoryShape
} from '../../../../helpers/propShapes';
import CategoryNameInput from '../../Molecules/CategoryCreationModal/CategoryNameInput';
import CategoryIconSelector from '../../Molecules/CategoryCreationModal/CategoryIconSelector';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const CategoryCreation = ({ api, categoryForm, close, mockCategoriesProps }) => {
  const { mockedCategories, addMockedCategory } = mockCategoriesProps;
  const { t } = useTranslation();
  const { templateCategories } = useTemplateCategories(api.credentialTypesManager);
  const [isLoading, setIsLoading] = useState(false);

  // This ref is used to focus on the input field when opening the modal
  const inputRef = useRef(null);

  useEffect(() => {
    if (inputRef && inputRef.current) {
      const { input } = inputRef.current;
      input.focus();
    }
  }, []);

  const validateNewCategory = () =>
    categoryForm.validateFields().catch(({ errorFields, values }) => ({
      errors: errorFields.map(errorField => errorField.errors),
      values
    }));

  const displayErrors = errors => errors.map(msg => message.error(t(msg)));

  const normalizeCategoryForm = ({ categoryName, ...rest }) => ({
    categoryName: categoryName.trim(),
    ...rest
  });

  const handleCategorySubmit = async () => {
    setIsLoading(true);
    const { errors, ...values } = await validateNewCategory();
    const isPartiallyValid = !errors?.length;
    if (!isPartiallyValid) {
      displayErrors(errors);
    } else {
      categoryForm.resetFields();
      const normalizedValues = normalizeCategoryForm(values);
      // TODO: remove when backend implements template categories
      addMockedCategory(normalizedValues);
      await api.credentialTypesManager.createCategory(normalizedValues);
      close();
    }
    setIsLoading(false);
  };

  return (
    <>
      <CategoryNameInput
        inputRef={inputRef}
        templateCategories={templateCategories}
        mockedCategories={mockedCategories}
      />
      <CategoryIconSelector categoryForm={categoryForm} />
      <div className="buttonSection">
        <CustomButton
          buttonText={t(`${i18nPrefix}.categoryCreationModal.save`)}
          loading={isLoading}
          buttonProps={{
            className: 'theme-secondary',
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
  close: PropTypes.func.isRequired,
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default withApi(CategoryCreation);
