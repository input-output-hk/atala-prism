import React from 'react';
import IdIcon from '../../../../images/IdIcon.svg';
import RowInfo from '../../atoms/row/row';
import './_style.scss';

const DetailBox = () => (
  <div className="detailBox">
    <div className="infoRow">
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
      <RowInfo contacts="30" groupName="Group Name Example 4" theme="row-info" />
    </div>
  </div>
);

export default DetailBox;
