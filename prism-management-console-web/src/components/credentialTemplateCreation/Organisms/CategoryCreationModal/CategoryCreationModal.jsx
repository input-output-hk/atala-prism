import React from 'react';
import PropTypes from 'prop-types';
import { Form, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import CategoryCreation from './CategoryCreation';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import './_style.scss';
import { defaultCategoryIcons } from '../../../../helpers/templateCategories/categories';

const i18nPrefix = 'credentialTemplateCreation';

const CategoryCreationModal = ({ visible, close, mockCategoriesProps }) => {
  const { t } = useTranslation();
  const [categoryForm] = Form.useForm();

  const validateMessages = {
    required: t(`${i18nPrefix}.errors.required`)
  };

  const defaultValues = {
    categoryName: '',
    categoryIcon: { index: 0, src: defaultCategoryIcons[0], isCustomIcon: false }
  };

  return (
    <Form
      form={categoryForm}
      initialValues={defaultValues}
      name="category-form"
      requiredMark={false}
      layout="vertical"
      validateMessages={validateMessages}
    >
      <Modal
        className="templateModal"
        visible={visible}
        onCancel={close}
        title={t(`${i18nPrefix}.categoryCreationModal.title`)}
        destroyOnClose
        footer={null}
      >
        <CategoryCreation
          categoryForm={categoryForm}
          close={close}
          mockCategoriesProps={mockCategoriesProps}
        />
      </Modal>
    </Form>
  );
};

CategoryCreationModal.propTypes = {
  visible: PropTypes.bool.isRequired,
  close: PropTypes.func.isRequired,
  mockCategoriesProps: PropTypes.shape({
    mockedCategories: templateCategoryShape,
    addMockedCategory: PropTypes.func.isRequired
  }).isRequired
};

export default CategoryCreationModal;
