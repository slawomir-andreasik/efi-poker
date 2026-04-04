import { getTaskSp, type TaskEstimate } from '@/components/ResultsTable';
import { isDefaultComment } from '@/hooks/useSortedTasks';

function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|');
}

function isConsensus(task: TaskEstimate): boolean {
  const nums = Object.values(task.estimates).filter((v): v is number => typeof v === 'number');
  return nums.length > 0 && new Set(nums).size === 1;
}

function blockquoteComment(comment: string): string {
  const escaped = escapeMarkdown(comment);
  return escaped
    .split('\n')
    .map((line) => `  > ${line}`)
    .join('\n');
}

function renderTask(
  task: TaskEstimate,
  participants: string[],
  lines: string[],
  commentTemplate?: string,
): void {
  lines.push(`### ${task.taskTitle}`);

  for (const name of participants) {
    const sp = task.estimates[name];
    const rawComment = task.comments[name];
    const comment =
      rawComment && commentTemplate && isDefaultComment(rawComment, commentTemplate)
        ? undefined
        : rawComment;

    if (sp == null) {
      lines.push(`- ${name}: -`);
    } else if (comment) {
      if (comment.includes('\n')) {
        lines.push(`- ${name}: **${sp}**`);
        lines.push(blockquoteComment(comment));
      } else {
        lines.push(`- ${name}: **${sp}** - ${escapeMarkdown(comment)}`);
      }
    } else {
      lines.push(`- ${name}: **${sp}**`);
    }
  }

  lines.push('');

  const parts: string[] = [];
  if (task.average != null) parts.push(`Avg: ${task.average.toFixed(1)}`);
  if (task.median != null) parts.push(`Med: ${task.median}`);
  if (task.finalEstimate != null) parts.push(`Final: ${task.finalEstimate}`);

  if (parts.length > 0) {
    lines.push(`*${parts.join(' | ')}*`);
    lines.push('');
  }
}

export function formatResultsAsMarkdown(
  roomTitle: string,
  tasks: TaskEstimate[],
  participants: string[],
  commentTemplate?: string,
): string {
  const lines: string[] = [`# Results: ${roomTitle}`, ''];

  if (tasks.length === 0) {
    return `${lines.join('\n').trimEnd()}\n`;
  }

  for (const task of tasks) renderTask(task, participants, lines, commentTemplate);

  const agreed = tasks.filter((t) => isConsensus(t));
  const totalSp = tasks.reduce((sum, t) => sum + getTaskSp(t), 0);
  lines.push('---');
  lines.push(
    `**Summary:** ${tasks.length} tasks | ${totalSp} SP | Consensus: ${agreed.length}/${tasks.length}`,
  );

  return `${lines.join('\n').trimEnd()}\n`;
}
