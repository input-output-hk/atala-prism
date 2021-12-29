/* eslint-disable react/prop-types */
import React from 'react';
import i18n from 'i18next';
import CellRenderer from '../../components/common/Atoms/CellRenderer/CellRenderer';
import DefaultIcon from '../../images/templates/genericUserIcon.svg';
import { backendDateFormat } from '../formatters';
import TemplatesActionButtons from '../../components/credentialTemplates/Atoms/Buttons/TemplatesActionButtons';
import PopOver from '../../components/common/Organisms/Detail/PopOver';

const translationKeyPrefix = 'templates.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

export const getTemplatesColumns = (templateCategories, tableActions) => {
  const unknownValue = i18n.t('templates.table.unknownValue');
  const getCategoryName = id => templateCategories.find(c => c.id === id)?.name;

  const baseColumns = [
    {
      key: 'icon',
      width: 25,
      render: ({ icon }) => (
        <img className="template-icon" src={icon || DefaultIcon} alt="template-icon" />
      )
    },
    {
      key: 'templateName',
      width: 150,
      render: ({ name }) => <CellRenderer title={tp('name')} value={name} />
    },
    {
      key: 'category',
      width: 150,
      render: ({ category }) => (
        <CellRenderer title={tp('category')} value={getCategoryName(category) || unknownValue} />
      )
    },
    {
      key: 'lastEdited',
      width: 100,
      render: ({ lastEdited }) => (
        <CellRenderer title={tp('lastEdited')} value={backendDateFormat(lastEdited?.seconds)} />
      )
    }
  ];

  const actionColumn = {
    key: 'actions',
    width: 150,
    align: 'right',
    render: template => {
      const { showTemplatePreview } = tableActions;
      const actionButtons = (
        <TemplatesActionButtons template={template} onPreview={showTemplatePreview} />
      );
      return <PopOver content={actionButtons} />;
    }
  };

  return baseColumns.concat(actionColumn);
};
