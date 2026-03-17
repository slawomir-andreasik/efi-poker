import type { TaskEstimate } from '@/components/ResultsTable';

function escapeMarkdown(text: string): string {
  return text.replace(/\|/g, '\\|');
}

function getTaskSp(task: TaskEstimate): number {
  if (task.finalEstimate != null) {
    const n = Number(task.finalEstimate);
    return isNaN(n) ? 0 : n;
  }
  return task.median ?? 0;
}

function isConsensus(task: TaskEstimate): boolean {
  const nums = Object.values(task.estimates).filter((v): v is number => typeof v === 'number');
  return nums.length > 0 && new Set(nums).size === 1;
}

function renderTask(task: TaskEstimate, participants: string[], lines: string[]): void {
  lines.push(`### ${task.taskTitle}`);

  for (const name of participants) {
    const sp = task.estimates[name];
    const comment = task.comments[name];

    if (sp == null) {
      lines.push(`- ${name}: -`);
    } else if (comment) {
      lines.push(`- ${name}: **${sp}** - ${escapeMarkdown(comment)}`);
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
    lines.push(`> ${parts.join(' | ')}`);
    lines.push('');
  }
}

export function formatResultsAsMarkdown(
  roomTitle: string,
  tasks: TaskEstimate[],
  participants: string[],
): string {
  const lines: string[] = [`# Results: ${roomTitle}`, ''];

  if (tasks.length === 0) {
    return lines.join('\n').trimEnd() + '\n';
  }

  const agreed = tasks.filter((t) => isConsensus(t));
  const needsDiscussion = tasks.filter((t) => !isConsensus(t));

  if (agreed.length > 0 && needsDiscussion.length > 0) {
    lines.push(`## Agreed (${agreed.length})`, '');
    for (const task of agreed) renderTask(task, participants, lines);

    lines.push(`## Needs Discussion (${needsDiscussion.length})`, '');
    for (const task of needsDiscussion) renderTask(task, participants, lines);
  } else {
    for (const task of tasks) renderTask(task, participants, lines);
  }

  const totalSp = tasks.reduce((sum, t) => sum + getTaskSp(t), 0);
  lines.push('---');
  lines.push(`**Summary:** ${tasks.length} tasks | ${totalSp} SP | Consensus: ${agreed.length}/${tasks.length}`);

  return lines.join('\n').trimEnd() + '\n';
}
