import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { antdV4FormShape } from '../../../../helpers/propShapes';
import CategoryNameInput from '../../Molecules/CategoryCreationModal/CategoryNameInput';
import CategoryIconSelector from '../../Molecules/CategoryCreationModal/CategoryIconSelector';
import { useTemplateStore } from '../../../../hooks/useTemplateStore';
import './_style.scss';

const i18nPrefix = 'credentialTemplateCreation';

const CategoryCreation = observer(({ categoryForm, close }) => {
  const { t } = useTranslation();

  const { templateCategories, addTemplateCategory } = useTemplateStore();

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
      const normalizedValues = normalizeCategoryForm(values);
      await addTemplateCategory(normalizedValues);
      close();
      categoryForm.resetFields();
    }
    setIsLoading(false);
  };

  return (
    <>
      <CategoryNameInput inputRef={inputRef} templateCategories={templateCategories} />
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
});

CategoryCreation.propTypes = {
  categoryForm: antdV4FormShape.isRequired,
  close: PropTypes.func.isRequired
};

export default CategoryCreation;
